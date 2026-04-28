insert into reference_master (category, code, name, description, extra_json, sort_order, is_active, created_at, updated_at)
values
('COUNTRY','IN','India','Country master',null,10,true,now(),now()),
('COUNTRY','AE','United Arab Emirates','Country master',null,20,true,now(),now()),
('ADDRESS_TYPE','BILLING','Billing Address','Address type',null,10,true,now(),now()),
('ADDRESS_TYPE','SHIPPING','Shipping Address','Address type',null,20,true,now(),now()),
('ADDRESS_TYPE','PICKUP','Pickup Address','Address type',null,30,true,now(),now()),
('REGION','IN-SOUTH','South India','Region','{"countryCode":"IN"}',10,true,now(),now()),
('REGION','IN-NORTH','North India','Region','{"countryCode":"IN"}',20,true,now(),now()),
('ZONE','KA-BLR-URBAN','Bangalore Urban Zone','Zone','{"regionCode":"IN-SOUTH"}',10,true,now(),now()),
('ZONE','DL-NCR','Delhi NCR Zone','Zone','{"regionCode":"IN-NORTH"}',20,true,now(),now()),
('STATE','KA','Karnataka','State','{"countryCode":"IN"}',10,true,now(),now()),
('STATE','MH','Maharashtra','State','{"countryCode":"IN"}',20,true,now(),now()),
('STATE','DL','Delhi','State','{"countryCode":"IN"}',30,true,now(),now()),
('CITY','BLR','Bengaluru','City','{"stateCode":"KA","countryCode":"IN","regionCode":"IN-SOUTH","zoneCode":"KA-BLR-URBAN"}',10,true,now(),now()),
('CITY','MUM','Mumbai','City','{"stateCode":"MH","countryCode":"IN","regionCode":"IN-SOUTH"}',20,true,now(),now()),
('CITY','DEL','Delhi','City','{"stateCode":"DL","countryCode":"IN","regionCode":"IN-NORTH","zoneCode":"DL-NCR"}',30,true,now(),now()),
('POSTAL_CODE','560001','560001','Postal code','{"cityCode":"BLR","stateCode":"KA","countryCode":"IN","regionCode":"IN-SOUTH","zoneCode":"KA-BLR-URBAN"}',10,true,now(),now()),
('POSTAL_CODE','400001','400001','Postal code','{"cityCode":"MUM","stateCode":"MH","countryCode":"IN","regionCode":"IN-SOUTH"}',20,true,now(),now()),
('POSTAL_CODE','110001','110001','Postal code','{"cityCode":"DEL","stateCode":"DL","countryCode":"IN","regionCode":"IN-NORTH","zoneCode":"DL-NCR"}',30,true,now(),now())
on conflict (category, code) do nothing;