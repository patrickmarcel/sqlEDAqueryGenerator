package fr.univtours.info;

import com.alexscode.utilities.json.JsonObj;
import fr.univtours.info.queries.AbstractEDAsqlQuery;

import javax.management.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
/*
{
    "metadata": {
        "kernelspec": {
            "name": "SQL",
            "display_name": "SQL",
            "language": "sql"
        },
        "language_info": {
            "name": "sql",
            "version": ""
        }
    },
    "nbformat_minor": 2,
    "nbformat": 4,
    "cells": [
        {
            "cell_type": "markdown",
            "source": [
                "# Demo Genration"
            ]
        },
        {
            "cell_type": "code",
            "source": [
                "SELECT COUNT(*) FROM covid;"
            ]
        }
    ]
}
 */

public class Notebook {
    List<AbstractEDAsqlQuery> queries;
    JsonObj root;
    ArrayList<JsonObj> cells;

    public Notebook(){
        queries = new ArrayList<>();
        cells = new ArrayList<>();
        root = new JsonObj();

        JsonObj language_info = new JsonObj();
        language_info.addNode("name", "sql");
        language_info.addNode("version", "");
        JsonObj kernelspec = new JsonObj();
        kernelspec.addNode("name", "SQL");
        kernelspec.addNode("display_name", "sql");
        JsonObj metadata = new JsonObj();
        metadata.addNode("kernelspec", kernelspec);
        metadata.addNode("language_info", language_info);
        root.addNode("metadata", metadata);
        root.addNode("nbformat_minor", 2);
        root.addNode("nbformat", 4);
    }

    public boolean addQuery(AbstractEDAsqlQuery q){
        return queries.add(q);
    }


    public String toJson(){
        for (AbstractEDAsqlQuery q : queries){
            JsonObj  cell = new JsonObj();
            cell.addNode("cell_type", "code");
            List<String> source =  q.getSql().lines().collect(Collectors.toList());
            cell.addNode("source", source);
            cells.add(cell);
        }
        root.addNode("cells", cells);
        return root.toString();
    }

}
