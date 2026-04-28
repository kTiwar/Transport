package com.tms.edi.routing.service;

import com.tms.edi.routing.config.RoutingProperties;
import com.tms.edi.routing.dto.OptimizeRoutesRequest;
import com.tms.edi.routing.dto.RoutingRouteResponse;
import com.tms.edi.routing.dto.RoutingStopResponse;
import com.tms.edi.routing.entity.RoutingDeliveryOrder;
import com.tms.edi.routing.entity.RoutingLocation;
import com.tms.edi.routing.entity.RoutingRoute;
import com.tms.edi.routing.entity.RoutingRouteStop;
import com.tms.edi.routing.entity.RoutingVehicle;
import com.tms.edi.routing.event.RoutesPlannedEvent;
import com.tms.edi.routing.event.RoutingKafkaPublisher;
import com.tms.edi.routing.optimizer.OrToolsVrpOptimizer;
import com.tms.edi.routing.optimizer.OrToolsVrpOptimizer.VrpProblem;
import com.tms.edi.routing.optimizer.OrToolsVrpOptimizer.VrpResult;
import com.tms.edi.routing.repository.RoutingDeliveryOrderRepository;
import com.tms.edi.routing.repository.RoutingRouteRepository;
import com.tms.edi.routing.repository.RoutingVehicleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteOptimizationOrchestrator {

    private final RoutingDeliveryOrderRepository orderRepo;
    private final RoutingVehicleRepository vehicleRepo;
    private final RoutingRouteRepository routeRepo;
    private final GeocodingService geocodingService;
    private final DistanceMatrixService distanceMatrixService;
    private final OrToolsVrpOptimizer optimizer;
    private final RoutingProperties routingProperties;
    private final RoutingKafkaPublisher routingKafkaPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<RoutingRouteResponse> optimize(OptimizeRoutesRequest req) {
        List<RoutingDeliveryOrder> orders = orderRepo.findByIdIn(req.getOrderIds());
        if (orders.size() != req.getOrderIds().size()) {
            throw new IllegalArgumentException("One or more order IDs not found.");
        }
        List<RoutingVehicle> vehicles = req.getVehicleIds().stream()
                .map(id -> vehicleRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + id)))
                .toList();

        double depotLat = req.getDepotLatitude() != null ? req.getDepotLatitude() : routingProperties.getDefaultDepotLatitude();
        double depotLon = req.getDepotLongitude() != null ? req.getDepotLongitude() : routingProperties.getDefaultDepotLongitude();
        RoutingLocation depotLoc = geocodingService.ensurePoint("Depot", depotLat, depotLon);

        List<double[]> latLon = new ArrayList<>();
        latLon.add(new double[] { depotLat, depotLon });
        List<Long> locationIdByNode = new ArrayList<>();
        locationIdByNode.add(depotLoc.getId());

        List<OrderNodeMap> jobNodes = new ArrayList<>();
        int idx = 1;
        for (RoutingDeliveryOrder o : orders) {
            RoutingLocation pLoc = o.getPickupLocation();
            if (pLoc == null) {
                pLoc = geocodingService.resolveAndCache(o.getPickupAddress(), o.getPickupPostcode());
                o.setPickupLocation(pLoc);
            }
            RoutingLocation dLoc = o.getDeliveryLocation();
            if (dLoc == null) {
                dLoc = geocodingService.resolveAndCache(o.getDeliveryAddress(), o.getDeliveryPostcode());
                o.setDeliveryLocation(dLoc);
            }
            orderRepo.save(o);

            int pNode = idx++;
            int dNode = idx++;
            latLon.add(new double[] { pLoc.getLatitude(), pLoc.getLongitude() });
            latLon.add(new double[] { dLoc.getLatitude(), dLoc.getLongitude() });
            locationIdByNode.add(pLoc.getId());
            locationIdByNode.add(dLoc.getId());
            jobNodes.add(new OrderNodeMap(o.getId(), pNode, dNode, Math.max(1, Math.round(o.getWeightKg()))));
        }

        var matrices = distanceMatrixService.buildMatrices(latLon);
        int n = latLon.size();
        long[][] dist = matrices.distanceMeters();
        long[][] dur = matrices.durationSeconds();

        long horizon = routingProperties.getTimeHorizonSeconds();
        long[] twMin = new long[n];
        long[] twMax = new long[n];
        Arrays.fill(twMin, 0);
        Arrays.fill(twMax, horizon);
        twMax[0] = Math.min(horizon, 86_400L);

        ZoneId zone = ZoneId.of(routingProperties.getRoutingTimeZoneId());
        ZonedDateTime dayStart = req.getRouteDate().atStartOfDay(zone);
        for (int i = 0; i < orders.size(); i++) {
            applyOrderTimeWindows(orders.get(i), dayStart, zone, horizon, twMin, twMax, jobNodes.get(i));
        }

        long[] demands = new long[n];
        demands[0] = 0;
        for (OrderNodeMap j : jobNodes) {
            demands[j.pickupNode()] = j.demandKg();
            demands[j.deliveryNode()] = -j.demandKg();
        }

        long[] svc = new long[n];
        int st = routingProperties.getServiceTimeSeconds();
        for (OrderNodeMap j : jobNodes) {
            svc[j.pickupNode()] = st;
            svc[j.deliveryNode()] = st;
        }

        long[] caps = vehicles.stream().mapToLong(v -> Math.max(1, Math.round(v.getCapacityWeightKg()))).toArray();
        int[][] pairs = jobNodes.stream().map(j -> new int[] { j.pickupNode(), j.deliveryNode() }).toArray(int[][]::new);

        VrpResult result = optimizer.solve(new VrpProblem(
                dist,
                dur,
                svc,
                twMin,
                twMax,
                horizon,
                routingProperties.getTimeSlackSeconds(),
                vehicles.size(),
                caps,
                demands,
                pairs));
        if (!result.ok()) {
            throw new IllegalStateException(result.error() != null ? result.error() : "VRP failed");
        }

        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<Integer, OrderNodeMap> pickupOf = jobNodes.stream().collect(Collectors.toMap(OrderNodeMap::pickupNode, j -> j));
        Map<Integer, OrderNodeMap> dropOf = jobNodes.stream().collect(Collectors.toMap(OrderNodeMap::deliveryNode, j -> j));

        List<RoutingRouteResponse> out = new ArrayList<>();
        List<Long> createdRouteIds = new ArrayList<>();
        for (int v = 0; v < vehicles.size(); v++) {
            List<Integer> nodes = result.routeNodesPerVehicle().get(v);
            if (nodes == null || nodes.size() < 3) {
                continue;
            }
            RoutingVehicle veh = vehicles.get(v);
            RoutingRoute route = RoutingRoute.builder()
                    .vehicle(veh)
                    .routeDate(req.getRouteDate())
                    .totalDistanceM((double) legSum(nodes, dist))
                    .totalDurationS((double) legSum(nodes, dur))
                    .status("PLANNED")
                    .optimizerRunId(runId)
                    .build();

            List<RoutingRouteStop> stops = buildStopsWithSchedule(
                    route, nodes, dist, dur, svc, pickupOf, dropOf, locationIdByNode, dayStart);
            route.setStops(stops);
            RoutingRoute saved = routeRepo.save(route);
            createdRouteIds.add(saved.getRouteId());
            out.add(toResponse(saved));
        }

        orders.forEach(o -> o.setStatus("ROUTED"));
        orderRepo.saveAll(orders);

        routingKafkaPublisher.publishRoutesPlanned(RoutesPlannedEvent.of(
                runId,
                createdRouteIds,
                vehicles.stream().map(RoutingVehicle::getVehicleId).toList(),
                req.getOrderIds(),
                req.getRouteDate().toString()));

        return out;
    }

    private static void applyOrderTimeWindows(
            RoutingDeliveryOrder o,
            ZonedDateTime dayStart,
            ZoneId zone,
            long horizon,
            long[] twMin,
            long[] twMax,
            OrderNodeMap nodes) {
        OffsetDateTime start = o.getTimeWindowStart();
        OffsetDateTime end = o.getTimeWindowEnd();
        if (start == null && end == null) {
            return;
        }
        long minS = start != null ? secondsSinceDayStart(dayStart, start, zone) : 0;
        long maxS = end != null ? secondsSinceDayStart(dayStart, end, zone) : horizon;
        if (maxS < minS) {
            long t = minS;
            minS = maxS;
            maxS = t;
        }
        minS = clamp(minS, 0, horizon);
        maxS = clamp(Math.max(maxS, minS), minS, horizon);
        twMin[nodes.pickupNode()] = minS;
        twMax[nodes.pickupNode()] = maxS;
        twMin[nodes.deliveryNode()] = minS;
        twMax[nodes.deliveryNode()] = maxS;
    }

    private static long secondsSinceDayStart(ZonedDateTime dayStart, OffsetDateTime odt, ZoneId zone) {
        ZonedDateTime zdt = odt.atZoneSameInstant(zone);
        return Duration.between(dayStart, zdt).getSeconds();
    }

    private static long clamp(long v, long lo, long hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private List<RoutingRouteStop> buildStopsWithSchedule(
            RoutingRoute route,
            List<Integer> nodes,
            long[][] dist,
            long[][] dur,
            long[] svc,
            Map<Integer, OrderNodeMap> pickupOf,
            Map<Integer, OrderNodeMap> dropOf,
            List<Long> locationIdByNode,
            ZonedDateTime dayStart) {
        List<RoutingRouteStop> stops = new ArrayList<>();
        ZonedDateTime cursor = dayStart;
        int seq = 0;
        for (int i = 1; i < nodes.size(); i++) {
            int from = nodes.get(i - 1);
            int to = nodes.get(i);
            cursor = cursor.plusSeconds(dur[from][to]);
            OffsetDateTime arrival = cursor.toOffsetDateTime();
            OffsetDateTime departure;
            if (to == 0) {
                departure = arrival;
            } else {
                departure = cursor.plusSeconds(svc[to]).toOffsetDateTime();
                cursor = cursor.plusSeconds(svc[to]);
            }
            RoutingDeliveryOrder orderRef = orderRefForNode(to, pickupOf, dropOf);
            RoutingRouteStop stop = RoutingRouteStop.builder()
                    .route(route)
                    .sequenceNumber(seq++)
                    .stopType(nodeType(to, pickupOf, dropOf))
                    .order(orderRef)
                    .location(entityManager.getReference(RoutingLocation.class, locationIdByNode.get(to)))
                    .travelTimeS((double) dur[from][to])
                    .distanceM((double) dist[from][to])
                    .arrivalTime(arrival)
                    .departureTime(departure)
                    .build();
            stops.add(stop);
        }
        return stops;
    }

    private RoutingDeliveryOrder orderRefForNode(int node, Map<Integer, OrderNodeMap> pickupOf, Map<Integer, OrderNodeMap> dropOf) {
        OrderNodeMap j = pickupOf.get(node);
        if (j != null) {
            return entityManager.getReference(RoutingDeliveryOrder.class, j.orderId());
        }
        j = dropOf.get(node);
        if (j != null) {
            return entityManager.getReference(RoutingDeliveryOrder.class, j.orderId());
        }
        return null;
    }

    private static String nodeType(int node, Map<Integer, OrderNodeMap> pickupOf, Map<Integer, OrderNodeMap> dropOf) {
        if (node == 0) {
            return "DEPOT";
        }
        if (pickupOf.containsKey(node)) {
            return "PICKUP";
        }
        if (dropOf.containsKey(node)) {
            return "DELIVERY";
        }
        return "STOP";
    }

    private static long legSum(List<Integer> nodes, long[][] m) {
        long s = 0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            s += m[nodes.get(i)][nodes.get(i + 1)];
        }
        return s;
    }

    private RoutingRouteResponse toResponse(RoutingRoute r) {
        return RoutingRouteResponse.builder()
                .routeId(r.getRouteId())
                .vehicleId(r.getVehicle().getVehicleId())
                .vehicleCode(r.getVehicle().getCode())
                .routeDate(r.getRouteDate())
                .totalDistanceM(r.getTotalDistanceM())
                .totalDurationS(r.getTotalDurationS())
                .status(r.getStatus())
                .optimizerRunId(r.getOptimizerRunId())
                .createdAt(r.getCreatedAt())
                .stops(r.getStops().stream().map(this::toStop).toList())
                .build();
    }

    private RoutingStopResponse toStop(RoutingRouteStop s) {
        RoutingLocation loc = s.getLocation();
        return RoutingStopResponse.builder()
                .stopId(s.getStopId())
                .sequenceNumber(s.getSequenceNumber())
                .stopType(s.getStopType())
                .orderId(s.getOrder() != null ? s.getOrder().getId() : null)
                .locationId(loc != null ? loc.getId() : null)
                .latitude(loc != null ? loc.getLatitude() : null)
                .longitude(loc != null ? loc.getLongitude() : null)
                .arrivalTime(s.getArrivalTime())
                .departureTime(s.getDepartureTime())
                .travelTimeS(s.getTravelTimeS())
                .distanceM(s.getDistanceM())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RoutingRouteResponse> listRoutes() {
        List<RoutingRoute> all = routeRepo.findAllByOrderByCreatedAtDesc();
        for (RoutingRoute r : all) {
            r.getVehicle().getCode();
            for (RoutingRouteStop s : r.getStops()) {
                if (s.getLocation() != null) {
                    s.getLocation().getLatitude();
                }
                if (s.getOrder() != null) {
                    s.getOrder().getId();
                }
            }
        }
        return all.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoutingRouteResponse getRoute(Long id) {
        RoutingRoute r = routeRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Route not found"));
        r.getVehicle().getCode();
        for (RoutingRouteStop s : r.getStops()) {
            if (s.getLocation() != null) {
                s.getLocation().getLatitude();
            }
        }
        return toResponse(r);
    }

    private record OrderNodeMap(long orderId, int pickupNode, int deliveryNode, long demandKg) {}
}
