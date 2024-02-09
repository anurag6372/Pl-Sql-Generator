package com.example.demo;

import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

@Service
public class OASJsonGenerator {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    public static Map<String,Object> allSchema =new HashMap<>();


    public Map<String,Object> generateRetrieveQueryList(){
        List<String> tempList =  List.of(
                "oas_contact",
                "oas_discriminator",
                "oas_encoding",
                "oas_example",
                "oas_external_documentation",
                "oas_header",
                "oas_info",
                "oas_license",
                "oas_link",
                "oas_media_type",
                "oas_oauth_flow",
                "oas_openapi",
                "oas_operation",
                "oas_parameter",
                "oas_path",
                "oas_request_body",
                "oas_response",
                "oas_schema",
                "oas_security_requirement",
                "oas_security_scheme",
                "oas_server",
                "oas_tag",
                "oas_xml"
        );

        Map<String,List<Map<String, Object>>> tempMap = new HashMap<>();
        for(String itr:tempList){
            tempMap.put(itr,  jdbcTemplate.queryForList("select * from config."+itr+" ;"));
        }
        for (Map.Entry<String, List<Map<String, Object>>> itr:tempMap.entrySet()){
            allSchema.put(itr.getKey(),schemaObjectFormatter(itr.getValue()));
        }
        return allSchema;
    }
    public Map<UUID, Object> schemaObjectFormatter(List<Map<String,Object>> maps){
        Map<UUID,Object> subTempMap = new HashMap<>();
        for (Map<String,Object> itr :maps){

            V3ReplicationProtocolHandler(itr);

            subTempMap.put((UUID) itr.get("id"),itr);
        }

        return subTempMap;
    }
    public static Map<String,Object> V3ReplicationProtocolHandler(Map<String,Object> temp){
        for (Map.Entry<String, Object> itr :temp.entrySet()){
            if(itr.getValue()!=null && itr.getValue() instanceof PgArray){
                try {
                    temp.put(itr.getKey(),new ArrayList<>(Arrays.asList((Object[]) ((PgArray) itr.getValue()).getArray())));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (itr.getValue()!=null && itr.getValue() instanceof PGobject){
                temp.put(itr.getKey(),((PGobject) itr.getValue()).getValue());
            }
        }
        return temp;
    }
}
