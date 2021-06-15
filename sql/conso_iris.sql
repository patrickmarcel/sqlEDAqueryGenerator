create table conso_iris
(
	"Année" integer,
	"Code IRIS" varchar(30),
	"Nom IRIS" text,
	"Type IRIS" text,
	"Code Commune" integer,
	"Nom Commune" text,
	"Code EPCI" varchar(30),
	"Nom EPCI" text,
	"Type EPCI" text,
	"Code Département" integer,
	"Nom Département" text,
	"CODE CATEGORIE CONSOMMATION" text,
	"CODE GRAND SECTEUR" text,
	"CODE SECTEUR NAF2" integer,
	"Nb sites" integer,
	"Conso totale (MWh)" numeric,
	"Conso moyenne (MWh)" numeric,
	"Part thermosensible (%)" numeric,
	"Conso totale  usages thermosensibles (MWh)" numeric,
	"Conso totale  usages non thermosensibles (MWh)" numeric,
	"Thermosensibilité totale (kWh/DJU)" numeric,
	"Conso totale corrigée de l'aléa climatique  usages thermosens" numeric,
	"Conso moyenne usages thermosensibles (MWh)" numeric,
	"Conso moyenne  usages non thermosensibles (MWh)" numeric,
	"Thermosensibilité moyenne (kWh/DJU)" numeric,
	"Conso moyenne corrigée de l'aléa climatique  usages thermosen" numeric,
	"DJU à TR" numeric,
	"DJU à TN" numeric,
	"Nombre d'habitants" integer,
	"Taux de logements collectifs" numeric,
	"Taux de résidences principales" numeric,
	"Superficie des logements < 30 m2" numeric,
	"Superficie des logements 30 à 40 m2" numeric,
	"Superficie des logements 40 à 60 m2" numeric,
	"Superficie des logements 60 à 80 m2" numeric,
	"Superficie des logements 80 à 100 m2" numeric,
	"Superficie des logements > 100 m2" numeric,
	"Résidences principales avant 1919" numeric,
	"Résidences principales de 1919 à 1945" numeric,
	"Résidences principales de 1946 à 1970" numeric,
	"Résidences principales de 1971 à 1990" numeric,
	"Résidences principales de 1991 à 2005" numeric,
	"Résidences principales de 2006 à 2010" numeric,
	"Résidences principales après 2011" numeric,
	"Taux de chauffage électrique" numeric
);

create index "conso_iris_nom département_index"
    on public.conso_iris ("Nom Département");

create index "conso_iris_nom commune_index"
    on public.conso_iris ("Nom Commune");


