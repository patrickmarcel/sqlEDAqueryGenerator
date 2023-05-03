# Setup database
Create farm and database
```shell
monetdbd create /home/alex/monetdb/covid
monetdbd start /home/alex/monetdb/covid
monetdb create covid
monetdb release covid
```

# Start existing
```shell
monetdbd start /home/alex/monetdb/covid
```


# Import/Export

```sql
-- Postgresql
copy "Rate" to '/tmp/Rate.tsv' with delimiter E'\t' NULL as 'NULL';
-- MonetDB
COPY INTO Rate FROM '/tmp/Rate.tsv' ON CLIENT USING DELIMITERS E'\t', E'\n', '"' NULL as 'NULL';
```