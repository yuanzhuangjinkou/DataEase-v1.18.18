package io.dataease.plugins.datasource.dm.service;

import io.dataease.plugins.common.constants.DatabaseClassification;
import io.dataease.plugins.common.constants.DatasourceCalculationMode;
import io.dataease.plugins.common.dto.StaticResource;
import io.dataease.plugins.common.dto.datasource.DataSourceType;
import io.dataease.plugins.datasource.service.DatasourceService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrestoService extends DatasourceService {


    @Override
    public List<String> components() {
        List<String> result = new ArrayList<>();
        result.add("presto");
        return result;
    }
    @Override
    protected InputStream readContent(String s) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("static/" + s);
        return resourceAsStream;
    }

    @Override
    public List<StaticResource> staticResources() {
        List<StaticResource> results = new ArrayList<>();
        StaticResource staticResource = new StaticResource();
        staticResource.setName("presto");
        staticResource.setSuffix("jpg");
        results.add(staticResource);
        results.add(pluginSvg());
        return results;
    }

    @Override
    public DataSourceType getDataSourceType() {
        DataSourceType dataSourceType =  new DataSourceType("presto", "Presto" , true , "", DatasourceCalculationMode.DIRECT, true);
        dataSourceType.setDatabaseClassification(DatabaseClassification.OLAP);
        return dataSourceType;
    }

    private StaticResource pluginSvg() {
        StaticResource staticResource = new StaticResource();
        staticResource.setName("presto-backend");
        staticResource.setSuffix("svg");
        return staticResource;
    }
}
