package com.tms.edi.routing.optimizer;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import com.tms.edi.routing.config.RoutingProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Capacitated VRP with pickup & delivery + {@link RoutingDimension} "Time" for travel, service, waiting, and TWs.
 * Distance matrix drives the objective; time matrix + service times drive feasibility with time windows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrToolsVrpOptimizer {

    private final RoutingProperties routingProperties;
    private boolean ortoolsLoaded;

    @PostConstruct
    void loadNative() {
        try {
            Loader.loadNativeLibraries();
            ortoolsLoaded = true;
            log.info("Google OR-Tools native libraries loaded for routing optimizer.");
        } catch (Throwable t) {
            ortoolsLoaded = false;
            log.error("OR-Tools failed to load native libraries — routing optimization disabled: {}", t.getMessage());
        }
    }

    public VrpResult solve(VrpProblem problem) {
        if (!ortoolsLoaded) {
            return VrpResult.failed("OR-Tools natives not available on this host.");
        }
        int n = problem.distanceMatrixMeters().length;
        if (n < 2 || problem.vehicleCount() < 1) {
            return VrpResult.failed("Invalid problem size.");
        }
        int depot = 0;
        RoutingIndexManager manager =
                new RoutingIndexManager(n, problem.vehicleCount(), depot);
        RoutingModel routing = new RoutingModel(manager);

        final int distanceTransit = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            return problem.distanceMatrixMeters()[fromNode][toNode];
        });
        routing.setArcCostEvaluatorOfAllVehicles(distanceTransit);

        long maxDistLeg = 0;
        for (long[] row : problem.distanceMatrixMeters()) {
            for (long v : row) {
                maxDistLeg = Math.max(maxDistLeg, v);
            }
        }
        long distHorizon = maxDistLeg * (n + 2);
        routing.addDimension(distanceTransit, 0, distHorizon, true, "Distance");
        RoutingDimension distanceDimension = routing.getMutableDimension("Distance");
        distanceDimension.setGlobalSpanCostCoefficient(100);

        final int demandIndex = routing.registerUnaryTransitCallback((long fromIndex) -> {
            int node = manager.indexToNode(fromIndex);
            return problem.demands()[node];
        });
        routing.addDimensionWithVehicleCapacity(
                demandIndex,
                0,
                problem.vehicleCapacities(),
                true,
                "Capacity");

        Solver solver = routing.solver();
        for (int[] pair : problem.pickupDeliveryNodePairs()) {
            long pickupIndex = manager.nodeToIndex(pair[0]);
            long deliveryIndex = manager.nodeToIndex(pair[1]);
            routing.addPickupAndDelivery(pickupIndex, deliveryIndex);
            solver.addConstraint(
                    solver.makeEquality(routing.vehicleVar(pickupIndex), routing.vehicleVar(deliveryIndex)));
            solver.addConstraint(solver.makeLessOrEqual(
                    distanceDimension.cumulVar(pickupIndex), distanceDimension.cumulVar(deliveryIndex)));
        }

        // ── Time dimension (travel + service at origin node), waiting slack, TWs ─────────────
        long[][] dur = problem.durationMatrixSeconds();
        long[] svc = problem.serviceTimeSeconds();
        long horizon = problem.timeHorizonSeconds();
        long waitSlack = problem.timeSlackSeconds();

        final int timeTransit = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            long travel = dur[fromNode][toNode];
            long service = svc[fromNode];
            return travel + service;
        });
        routing.addDimension(timeTransit, (int) Math.min(waitSlack, Integer.MAX_VALUE), horizon, true, "Time");
        RoutingDimension timeDimension = routing.getMutableDimension("Time");

        long[] twMin = problem.timeWindowMinSeconds();
        long[] twMax = problem.timeWindowMaxSeconds();
        for (int node = 1; node < n; node++) {
            long idx = manager.nodeToIndex(node);
            long a = Math.min(twMin[node], twMax[node]);
            long b = Math.max(twMin[node], twMax[node]);
            a = Math.max(0, a);
            b = Math.min(horizon, Math.max(b, a));
            timeDimension.cumulVar(idx).setRange(a, b);
        }

        long depotStartMin = twMin[0];
        long depotStartMax = Math.max(depotStartMin, twMax[0]);
        depotStartMin = Math.max(0, depotStartMin);
        depotStartMax = Math.min(horizon, Math.max(depotStartMax, depotStartMin));
        for (int v = 0; v < problem.vehicleCount(); v++) {
            long startIdx = routing.start(v);
            timeDimension.cumulVar(startIdx).setRange(depotStartMin, depotStartMax);
        }

        for (int[] pair : problem.pickupDeliveryNodePairs()) {
            long pickupIndex = manager.nodeToIndex(pair[0]);
            long deliveryIndex = manager.nodeToIndex(pair[1]);
            solver.addConstraint(solver.makeLessOrEqual(
                    timeDimension.cumulVar(pickupIndex), timeDimension.cumulVar(deliveryIndex)));
        }

        for (int v = 0; v < problem.vehicleCount(); v++) {
            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.start(v)));
            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.end(v)));
        }

        RoutingSearchParameters searchParameters =
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PARALLEL_CHEAPEST_INSERTION)
                        .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                        .setTimeLimit(com.google.protobuf.Duration.newBuilder()
                                .setSeconds(routingProperties.getOptimizerTimeLimitSeconds())
                                .build())
                        .build();

        Assignment solution = routing.solveWithParameters(searchParameters);
        if (solution == null) {
            return VrpResult.failed("No feasible VRP solution (capacity, pickup-drop, distance, or time windows).");
        }

        List<List<Integer>> perVehicle = new ArrayList<>();
        for (int v = 0; v < problem.vehicleCount(); v++) {
            List<Integer> nodes = new ArrayList<>();
            long index = routing.start(v);
            while (!routing.isEnd(index)) {
                nodes.add(manager.indexToNode(index));
                index = solution.value(routing.nextVar(index));
            }
            nodes.add(manager.indexToNode(index));
            if (nodes.size() > 2 || (nodes.size() == 2 && !(nodes.get(0) == depot && nodes.get(1) == depot))) {
                perVehicle.add(nodes);
            } else {
                perVehicle.add(List.of());
            }
        }

        return new VrpResult(true, solution.objectiveValue(), perVehicle, null);
    }

    public record VrpProblem(
            long[][] distanceMatrixMeters,
            long[][] durationMatrixSeconds,
            long[] serviceTimeSeconds,
            long[] timeWindowMinSeconds,
            long[] timeWindowMaxSeconds,
            long timeHorizonSeconds,
            long timeSlackSeconds,
            int vehicleCount,
            long[] vehicleCapacities,
            long[] demands,
            int[][] pickupDeliveryNodePairs
    ) {}

    public record VrpResult(
            boolean ok,
            long objectiveMeters,
            List<List<Integer>> routeNodesPerVehicle,
            String error
    ) {
        static VrpResult failed(String msg) {
            return new VrpResult(false, 0, List.of(), msg);
        }
    }
}
