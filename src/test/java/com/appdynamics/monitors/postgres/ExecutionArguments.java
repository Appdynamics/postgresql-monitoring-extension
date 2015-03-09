package com.appdynamics.monitors.postgres;

import java.util.List;

public class ExecutionArguments {
    private final List<String> columnNames;

    public ExecutionArguments(List<String> columnNames){

        this.columnNames = columnNames;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }
}
