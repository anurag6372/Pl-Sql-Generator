package com.example.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is responsible for generating SQL blocks based on JSON schemas.
 */
@Service
public class ScriptGenerator {
    public final static Map<String,String> staticKeywords = Map.ofEntries(
            Map.entry("discriminator","config.oas_discriminator"),
            Map.entry("xml","config.oas_xml"),
            Map.entry("externalDocs","config.oas_external_documentation")
    );

    public final static Map<String, String> propertyMap = Map.ofEntries(
            Map.entry("format", "format"),
            Map.entry("type", "type"),
            Map.entry("enum", "enum_field"),
            Map.entry("const", "const"),
            Map.entry("multipleOf", "multiple_of"),
            Map.entry("maximum", "maximum"),
            Map.entry("exclusiveMaximum", "exclusive_maximum"),
            Map.entry("minimum", "minimum"),
            Map.entry("exclusiveMinimum", "exclusive_minimum"),
            Map.entry("maxLength", "max_length"),
            Map.entry("minLength", "min_length"),
            Map.entry("pattern", "pattern"),
            Map.entry("maxItems", "max_items"),
            Map.entry("minItems", "min_items"),
            Map.entry("uniqueItems", "unique_items"),
            Map.entry("maxContains", "max_contains"),
            Map.entry("minContains", "min_contains"),
            Map.entry("maxProperties", "max_properties"),
            Map.entry("minProperties", "min_properties"),
            Map.entry("required", "required"),
            Map.entry("dependentRequired", "dependent_required"),
            Map.entry("$id", "__id"),
            Map.entry("$schema", "__schema"),
            Map.entry("$vocabulary", "__vocabulary"),
            Map.entry("$dynamicRef", "__dynamic_ref"),
            Map.entry("$dynamicAnchor", "__dynamic_anchor"),
            Map.entry("$anchor", "__anchor"),
            Map.entry("$ref", "__ref"),
            Map.entry("title", "title"),
            Map.entry("description", "description"),
            Map.entry("$comment", "__comment"),

            //single schemas
            Map.entry("unevaluatedItems", "unevaluated_items"),
            Map.entry("unevaluatedProperties", "unevaluated_properties"),
            Map.entry("not", "not_field"),
            Map.entry("if", "if_field"),
            Map.entry("then", "then_field"),
            Map.entry("else", "else_field"),
            Map.entry("items", "items"),
            Map.entry("additionalItems", "additional_items"),
            Map.entry("contains", "contains"),
            Map.entry("propertyNames", "property_names"),
            Map.entry("additionalProperties", "additional_properties"),

            //Array Schemas
            Map.entry("allOf", "all_of"),
            Map.entry("anyOf", "any_of"),
            Map.entry("oneOf", "one_of"),

            //Map Schemas
            Map.entry("$defs", "__defs"),
            Map.entry("properties", "properties"),
            Map.entry("patternProperties", "pattern_properties"),
            Map.entry("dependentSchemas", "dependent_schemas"),

            Map.entry("discriminator", "discriminator_id"),
            Map.entry("xml", "xml_id"),
            Map.entry("externalDocs", "external_docs_id"),
            Map.entry("example", "example"),
            Map.entry("specificationExtensions", "specification_extensions"),

            Map.entry("id", "id"),
            Map.entry("name", "name"),
            Map.entry("uri", "uri"),
            Map.entry("x-identifier", "x_identifier"),
            Map.entry("packageName", "package_name"),

            //discriminator properties
            Map.entry("propertyName", "property_name"),
            Map.entry("mapping", "mapping"),

            //xml properties
            Map.entry("namespace", "namespace"),
            Map.entry("prefix","prefix"),
            Map.entry("attribute","attribute"),
            Map.entry("wrapped","wrapped"),

            //external_Docs properties
            Map.entry("url", "url"),
            Map.entry("x-fullyQualifiedNameIdentifier","x_fully_qualified_name_identifier")
    );
    public static Set<String> arrayKeywords = new HashSet<>(Set.of("allOf", "anyOf", "oneOf"));
    public static Set<String> mapKeywords = new HashSet<>(Set.of("$defs", "properties", "patternProperties", "dependentSchemas"));
    public static Set<String> singleKeywords = new HashSet<>(Set.of("unevaluatedItems", "unevaluatedProperties",  "not", "if", "then", "else", "items", "additionalItems", "contains", "propertyNames", "additionalProperties"));
    static StringBuilder plsqlBlock = new StringBuilder();
    static ArrayList<String> insertSchemaQueries = new ArrayList<>();
    static ArrayList<String> insertMappingQueries = new ArrayList<>();
    static HashMap<Integer,String> visited = new HashMap<>();
    public final static Integer[] counter = new Integer[]{-1};

//    public static void main(String[] args) {
//        System.out.println((convertJsonToMap("employee_schema.json")));
//        plsqlBlockGenerator();
//
//    }
// ================================================================


    public static Map<String, Object> convertJsonToMap(String fileName) {
        Resource resource = new ClassPathResource(fileName);
        String content;
        Map<String, Object> jsonObject;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            InputStream inputStream = resource.getInputStream();
            content = new String(inputStream.readAllBytes());
            jsonObject = objectMapper.readValue(content, new TypeReference<>(){});
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return processJsonObject(jsonObject, jsonObject);
    }

    private static Map<String, Object> processJsonObject(Map<String, Object> jsonObject, Map<String, Object> rootMap) {
        for (Map.Entry<String, Object> entry: jsonObject.entrySet()) {
            if ("$ref".equals(entry.getKey())) {
                if (!(entry.getValue() instanceof Map<?,?>)) {
                    String [] referencePath = entry.getValue().toString().split("/");
                    if (referencePath.length == 1 && referencePath[0].equals("#")) {
                        return rootMap;
                    } else {
                        Map<String, Object> objectToBeResolved = rootMap;
                        for (String pathPart: referencePath) {
                            objectToBeResolved = resolveRefInJsonObject(pathPart, objectToBeResolved);
                        }
                        return objectToBeResolved;
                    }
                }
            } else if (entry.getValue() instanceof Map<?,?>) {
                entry.setValue(processJsonObject((Map<String, Object>) entry.getValue(), rootMap));
            } else if (entry.getValue() instanceof List<?>) {
                List<Object> arrayOfObjects = (List<Object>) entry.getValue();
                for (Object object: arrayOfObjects) {
                    if (object instanceof Map<?,?>)
                        arrayOfObjects.set(arrayOfObjects.indexOf(object), processJsonObject((Map<String, Object>) object, rootMap));
                }
            }
        }
        return jsonObject;
    }

    private static Map<String, Object> resolveRefInJsonObject(String pathPart, Map<String, Object> objectToBeResolved) {
        if (pathPart.equals("#")) {
            return objectToBeResolved;
        }
        else {
            if (objectToBeResolved.containsKey(pathPart)) {
                return (Map<String, Object>) objectToBeResolved.get(pathPart);
            } else {
                throw new RuntimeException("There is no key with the given " + pathPart);
            }
        }
    }

// ================================================================


    /**
     * Generates a PLSQL block based on the provided map.
     *And store it in a file
     */
    public static void plsqlBlockGenerator(){
        String fileName  = "employee_schema.json";
        extractMap(visited,convertJsonToMap(fileName),counter);
        plsqlBlock.append("DO $$\nDECLARE\n\n");
        String results = "  ";
        for(Map.Entry<Integer,String> itr:visited.entrySet()){
            results = results+itr.getValue()+" uuid := uuid_generate_v4();\n  ";
        }
        plsqlBlock.append(results+"\n\nBEGIN\n\n\n");
//        Collections.reverse(insertSchemaQueries);
        plsqlBlock.append(insertSchemaQueries.get(insertSchemaQueries.size()-1));
        for (int i = 0; i < (insertSchemaQueries.size()-1); i++) {
            plsqlBlock.append(insertSchemaQueries.get(i));
        }

        for (String query : insertMappingQueries){
            plsqlBlock.append(query);
        }
        plsqlBlock.append("\n\nEND $$");

        try {
            String directoryPath = "queries";

            // Create the directory if it doesn't exist
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs(); // This will create any missing parent directories as well
            }

            // Create the file in the specified directory
            File file = new File(directory, fileName+".sql");

            if(file.exists()) {
                FileWriter fileWriter = new FileWriter(directoryPath, false);
                fileWriter.close();
            }else{
                file.createNewFile();
            }

            PrintWriter pw = new PrintWriter(file);
            pw.println(plsqlBlock);
            pw.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(plsqlBlock);
    }

    /**
     * Builds an insert query for the given table and values.
     *
     * @param tableName The name of the table.
     * @param values    The values to insert.
     * @return The constructed insert query.
     */
    public static String buildInsertQuery(String tableName, Map<String,Object> values) {


        StringBuilder queryBuilder = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder valuesBuilder = new StringBuilder(") VALUES (");

        int index = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String columnName = propertyMap.containsKey(entry.getKey())?propertyMap.get(entry.getKey()):entry.getKey();

            Object columnValue = entry.getValue();
            if (columnValue instanceof List){
                columnValue = "{" +((List<?>)columnValue).stream()
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(","))+ "}";
            }
            if(columnValue instanceof String || columnValue instanceof Map<?,?>){
                if (!( columnValue.toString()).startsWith("idFor")) {
                    columnValue = "'" + columnValue + "'";
                }
            }


            queryBuilder.append(columnName);
            valuesBuilder.append(columnValue);

            if (index < values.size() - 1) {
                queryBuilder.append(", ");
                valuesBuilder.append(", ");
            }

            index++;
        }

        queryBuilder.append(valuesBuilder).append(");\n");

        return queryBuilder.toString();
    }

    /**
     * Extracts map information recursively and generates SQL insert queries.
     *
     * @param visited The visited map.
     * @param map     The map to extract information from.
     * @param counter The counter for generating unique IDs.
     * @return The extracted map id.
     */
    public static String extractMap(Map<Integer,String> visited, Map<String,Object> map, Integer[] counter){

        Map<String,Object> schemaValues = new HashMap<>();
        if(visited.containsKey(System.identityHashCode(map))){
            return visited.get(System.identityHashCode(map));
        }
        counter[0] = counter[0]+1;
        visited.put(System.identityHashCode(map),"idFor_"+counter[0]);


        for (Map.Entry<String,Object> entry : map.entrySet()){
            if (entry.getValue() instanceof Map<?, ?> && mapKeywords.contains(entry.getKey())){
                Map<String,Object> mappingValues = new HashMap<>();
                for (Map.Entry<?, ?> itr : ((Map<?, ?>) entry.getValue()).entrySet()){
                    if(!(visited.containsKey(System.identityHashCode(itr.getValue())))){

                        visited.put(System.identityHashCode(itr.getValue()),extractMap(visited, (Map<String, Object>) itr.getValue(), counter));

                    }

                    mappingValues.clear();
                    mappingValues.put("schema_field", entry.getKey());
                    mappingValues.put("parent_schema_id", visited.get(System.identityHashCode(map)));
                    mappingValues.put("child_schema_id",visited.get(System.identityHashCode(itr.getValue())));
                    mappingValues.put("mapping_name",itr.getKey());

//                  to generate the insert query
                    insertMappingQueries.add(buildInsertQuery("config.oas_parent_child_schema_mapping",mappingValues));


                }
            }
            else if (entry.getValue() instanceof List<?> && arrayKeywords.contains(entry.getKey())) {
                Map<String,Object> mappingValues = new HashMap<>();
                for (var itr :  (List)entry.getValue()){

                    if(!(visited.containsKey(System.identityHashCode(itr)))){
                        visited.put(System.identityHashCode(itr),extractMap(visited, (Map<String, Object>) itr, counter));

                    }

                    mappingValues.clear();
                    mappingValues.put("schema_field", entry.getKey());
                    mappingValues.put("parent_schema_id", visited.get(System.identityHashCode(map)));
                    mappingValues.put("child_schema_id",visited.get(System.identityHashCode(itr)));
                    mappingValues.put("mapping_name",null);

//                  to generate the insert query
                    insertMappingQueries.add(buildInsertQuery("config.oas_parent_child_schema_mapping",mappingValues));
                }

            }
            else if (entry.getValue() instanceof Map<?,?> && singleKeywords.contains((entry.getKey()))) {

                if(!(visited.containsKey(System.identityHashCode(entry.getValue())))){
                    visited.put(System.identityHashCode(entry.getValue()),extractMap(visited, (Map<String, Object>) entry.getValue(), counter));

                }
                schemaValues.put(entry.getKey(),visited.get(System.identityHashCode(entry.getValue())));
            }
            else if (entry.getValue() instanceof Map<?,?>  && staticKeywords.containsKey(entry.getKey())) {
                Map<String,Object> staticValues = new HashMap<>();
//                if(!(visited.containsKey(System.identityHashCode(entry.getValue())))){
//                    visited.put(System.identityHashCode(entry.getValue()),extractMap(visited, (Map<String, Object>) entry.getValue(), counter));
//
//                }
                String singleSchemaMapId = "idFor_"+(++counter[0]);
                for (Map.Entry<?, ?> e : ((Map<?, ?>) entry.getValue()).entrySet()) {
                    staticValues.put((String) e.getKey(), e.getValue());
                }
                staticValues.put("id",singleSchemaMapId);
                visited.put(System.identityHashCode(entry.getValue()),singleSchemaMapId);

//              to generate the insert query for static schema
                insertSchemaQueries.add(buildInsertQuery(staticKeywords.get(entry.getKey()), staticValues));


                schemaValues.put(entry.getKey(),singleSchemaMapId);
            }
            else {
                schemaValues.put(entry.getKey(),entry.getValue());
            }
        }

        schemaValues.putIfAbsent("id",visited.get(System.identityHashCode(map)));

        //      to generate the insert query
        insertSchemaQueries.add(buildInsertQuery("config.oas_schema", schemaValues));

        return (String) schemaValues.get("id");

    }








}