DO $$
DECLARE

  idFor_4 uuid := uuid_generate_v4();
  idFor_3 uuid := uuid_generate_v4();
  idFor_0 uuid := uuid_generate_v4();
  idFor_2 uuid := uuid_generate_v4();
  idFor_1 uuid := uuid_generate_v4();
  

BEGIN


INSERT INTO config.oas_schema (__schema, name, id, type, required, x_identifier) VALUES ('https://json-schema.org/draft/2020-12/schema', 'OperatorDto', '9d5aceaa-3ba1-4b50-a54f-a43ecf416329', 'object', '{"id","name","type"}', 'id');
INSERT INTO config.oas_schema (format, id, type) VALUES ('uuid', idFor_1, 'string');
INSERT INTO config.oas_schema (id, type) VALUES (idFor_2, 'string');
INSERT INTO config.oas_schema (id, type) VALUES (idFor_3, 'string');
INSERT INTO config.oas_schema (id, type) VALUES (idFor_4, 'boolean');
INSERT INTO config.oas_parent_child_schema_mapping (parent_schema_id, schema_field, mapping_name, child_schema_id) VALUES (idFor_0, 'properties', 'id', idFor_1);
INSERT INTO config.oas_parent_child_schema_mapping (parent_schema_id, schema_field, mapping_name, child_schema_id) VALUES (idFor_0, 'properties', 'name', idFor_2);
INSERT INTO config.oas_parent_child_schema_mapping (parent_schema_id, schema_field, mapping_name, child_schema_id) VALUES (idFor_0, 'properties', 'type', idFor_3);
INSERT INTO config.oas_parent_child_schema_mapping (parent_schema_id, schema_field, mapping_name, child_schema_id) VALUES (idFor_0, 'properties', 'isArchive', idFor_4);


END $$
