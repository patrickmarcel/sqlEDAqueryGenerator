package fr.univtours.info;


import fr.univtours.info.queries.AbstractEDAsqlQuery;

import java.util.ArrayList;
import java.util.List;

public class NotebookJupyter {
    List<AbstractEDAsqlQuery> queries;
    String dbUrl = "<db_url>";

    public NotebookJupyter(){
        queries = new ArrayList<>();
    }

    public NotebookJupyter(String baseURL) {
        queries = new ArrayList<>();
        dbUrl = baseURL.replaceFirst("jdbc:", "");
    }

    public boolean addQuery(AbstractEDAsqlQuery q){
        return queries.add(q);
    }

    private static String queryToJSON(AbstractEDAsqlQuery q){
        StringBuilder sb = new StringBuilder("{");
        sb.append(queryCellHeader);
        sb.append("   \"source\": [\n");
        sb.append("    \"%%sql\\n\",\n");

        String[] qlines = q.getSql().split("\\r?\\n");
        for (int j = 0; j < qlines.length; j++) {
            sb.append(getLineRepr(qlines[j]));
            if (j != qlines.length - 1)
                sb.append(",\n");
        }

        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private String queryDescriptor(AbstractEDAsqlQuery q, int qnb) {
        StringBuilder sb = new StringBuilder("  {\n" +
                "   \"cell_type\": \"markdown\",\n" +
                "   \"metadata\": {},\n" +
                "   \"source\": [");
        sb.append("\"### Q").append(qnb).append("\\n\",\n");
        sb.append("\"Interestingness = ").append(q.getInterest()).append("\\n\",\n");

        String[] qlines = q.getDescription().split("\\r?\\n");
        for (int j = 0; j < qlines.length; j++) {
            sb.append(getLineRepr(qlines[j]));
            if (j != qlines.length - 1)
                sb.append(",\n");
        }

        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    public String toJson(){
        StringBuilder sb = new StringBuilder("{\n");
        sb.append(" \"cells\": [\n");
        sb.append(titleCell).append(",\n");
        sb.append(loadCell.replace("<db_url>", dbUrl)).append(",\n");

        for (int i = 0; i < queries.size(); i++) {
            sb.append(queryDescriptor(queries.get(i), i + 1));
            sb.append(",\n");
            sb.append(queryToJSON(queries.get(i)));
            if (i < queries.size() - 1)
                sb.append(",\n");
        }

        sb.append(" ],\n");
        sb.append(metadata);
        sb.append("}");
        return sb.toString();
    }

    private static String getLineRepr(Object value) {
        String in = (String) value;
        String out = in.replace("\r\n", "\\r\\n").replace("\n", "\\n");
        return "\"" + out + " \\n\"";
    }

    private static final String metadata = " \"metadata\": {\n" +
            "  \"kernelspec\": {\n" +
            "   \"display_name\": \"Python 3\",\n" +
            "   \"language\": \"python\",\n" +
            "   \"name\": \"python3\"\n" +
            "  },\n" +
            "  \"language_info\": {\n" +
            "   \"codemirror_mode\": {\n" +
            "    \"name\": \"ipython\",\n" +
            "    \"version\": 3\n" +
            "   },\n" +
            "   \"file_extension\": \".py\",\n" +
            "   \"mimetype\": \"text/x-python\",\n" +
            "   \"name\": \"python\",\n" +
            "   \"nbconvert_exporter\": \"python\",\n" +
            "   \"pygments_lexer\": \"ipython3\",\n" +
            "   \"version\": \"3.8.5\"\n" +
            "  }\n" +
            " },\n" +
            " \"nbformat\": 4,\n" +
            " \"nbformat_minor\": 5";

    private static final String queryCellHeader = "\"cell_type\": \"code\",\n" +
            "   \"execution_count\": null,\n" +
            "   \"metadata\": {},\n" +
            "   \"outputs\": [],\n";

    private static final String loadCell = "{\n" +
            "   \"cell_type\": \"code\",\n" +
            "   \"execution_count\": null,\n" +
            "   \"id\": \"applied-backup\",\n" +
            "   \"metadata\": {},\n" +
            "   \"outputs\": [],\n" +
            "   \"source\": [\n" +
            "    \"import sqlalchemy\\n\",\n" +
            "    \"sqlalchemy.create_engine(\\\"<db_url>\\\")\\n\",\n" +
            "    \"%load_ext sql\\n\",\n" +
            "    \"%sql <db_url>\"\n" +
            "   ]\n" +
            "  }";

    private static final String titleCell = "  {\n" +
            "   \"cell_type\": \"markdown\",\n" +
            "   \"id\": \"frequent-consultancy\",\n" +
            "   \"metadata\": {},\n" +
            "   \"source\": [\n" +
            "    \"# TAP Story for <dataset_name>\\n\",\n" +
            "    \"This data analysis was automatically generated using the TAP algorithm.\"\n" +
            "   ]\n" +
            "  }";
}
