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
                "# TAP Notebook Demo"
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

    public Notebook(){
        queries = new ArrayList<>();
    }

    public boolean addQuery(AbstractEDAsqlQuery q){
        return queries.add(q);
    }


    public String toJson(){
        StringBuilder sb = new StringBuilder("{\n" +
                "    \"metadata\": {\n" +
                "        \"kernelspec\": {\n" +
                "            \"name\": \"SQL\",\n" +
                "            \"display_name\": \"SQL\",\n" +
                "            \"language\": \"sql\"\n" +
                "        },\n" +
                "        \"language_info\": {\n" +
                "            \"name\": \"sql\",\n" +
                "            \"version\": \"\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"nbformat_minor\": 2,\n" +
                "    \"nbformat\": 4,\n" +
                "    \"cells\": [");

        sb.append("        {\n" +
                "            \"cell_type\": \"markdown\",\n" +
                "            \"source\": [\n" +
                "                \"# TAP Notebook Demonstration\\n\",\n" +
                "                \"\\n\",\n" +
                "                \"Patrick Marcel, Alexandre Chanson, Nicolas Labroche, Vincent T'Kindt\"\n" +
                "            ]\n" +
                "        },\n");

        // for each query
        for (int i = 0; i < queries.size(); i++) {
            sb.append("        {\n" +
                    "            \"cell_type\": \"markdown\",\n" +
                    "            \"source\": [\n" +
                    "                \"### Q"+i+"\\n\",\n" +
                    "                \"\\n\",\n" +
                    "                \"I="+queries.get(i).getInterest()+ "\"\\n\",\n" + JsonObj.getLineRepr(queries.get(i).getDescription()) +
                    "            ]\n" +
                    "        },\n");


            sb.append("{\n" +
                    "            \"cell_type\": \"code\",\n" +
                    "            \"source\": [");
            String[] qlines = queries.get(i).getSql().split("\\r?\\n");
            for (int j = 0; j < qlines.length; j++) {
                sb.append(JsonObj.getLineRepr(qlines[j]));
                if (j != qlines.length - 1)
                    sb.append(",\n");
            }
            sb.append("]\n" +
                    "        }");
            if (i != queries.size() - 1)
                sb.append(",\n");
        }


        sb.append("    ]\n" +
                "}");


        return sb.toString();
    }

}
