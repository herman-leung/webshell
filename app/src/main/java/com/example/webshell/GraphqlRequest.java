package com.example.webshell;

public class GraphqlRequest {
    private String operationName;
    private Variables variables;
    private String query;

    public GraphqlRequest(String operationName, Variables variables, String query) {
        this.operationName = operationName;
        this.variables = variables;
        this.query = query;
    }

    public static class Variables {
        private int pageSize;
        private int currentPage;
        private String name;

        public Variables(int pageSize, int currentPage, String name) {
            this.pageSize = pageSize;
            this.currentPage = currentPage;
            this.name = name;
        }
    }
}