-- DATA PREPARATION
drop table if exists countries;
create table countries(country varchar, Alpha2 char(2), Alpha3 char(3), code int, Latitude float, Longitude float);

copy countries from 'C:\Users\achan\IdeaProjects\sqlEDAqueryGenerator\sql\countries-2.csv' (format csv,header,delimiter ';');

drop table if exists covid;
create table covid(dateRep date, day integer, month integer, year integer, cases integer, deaths integer, countriesAndTerritories varchar, geoId varchar, countryterritoryCode varchar, popData2018 integer, continentExp varchar);

set datestyle="DMY";
copy covid from 'C:\Users\achan\IdeaProjects\sqlEDAqueryGenerator\sql\COVID-19-geographic-disbtribution-worldwide-2020-05-29.csv'  (format CSV,header,delimiter ';');


drop table if exists covid19backup;

create table covid19backup(dateRep date, day integer, month integer, year integer, cases integer, deaths integer, countriesAndTerritories varchar, geoId varchar, countryterritoryCode varchar, popData2018 integer, continentExp varchar, cases100k double precision, cases1m double precision, deaths100k double precision, deaths1m double precision);

insert into covid19backup
(select c.*,
(cases/(popData2018/100000::float)) as cases100k,
(cases/(popData2018/1000000::float)) as cases1m,
(deaths/(popData2018/100000::float)) as deaths100k,
(deaths/(popData2018/1000000::float)) as deaths100k
from covid c);
