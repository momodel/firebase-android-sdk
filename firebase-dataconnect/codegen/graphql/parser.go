package graphql

import (
	"errors"
	pluralize "github.com/gertd/go-pluralize"
	"github.com/vektah/gqlparser/v2"
	"github.com/vektah/gqlparser/v2/ast"
	"github.com/vektah/gqlparser/v2/gqlerror"
	"github.com/vektah/gqlparser/v2/parser"
	"github.com/vektah/gqlparser/v2/validator"
	"log"
	"os"
	"strings"
)

func LoadSchemaFile(file string) (*ast.Schema, error) {
	sources, err := loadPreludeSources()
	if err != nil {
		return nil, err
	}

	log.Println("Loading GraphQL schema file:", file)
	source, err := loadGraphQLSourceFromFile(file)
	if err != nil {
		return nil, err
	}
	sources = append(sources, source)

	graphqlSchema, err := gqlparser.LoadSchema(sources...)
	if err != nil {
		return nil, err
	}

	err = addSynthesizedTypesAndFields(graphqlSchema)
	if err != nil {
		return nil, err
	}

	return graphqlSchema, nil
}

func addSynthesizedTypesAndFields(schema *ast.Schema) error {
	originalNonBuiltInTypes := nonBuiltInTypesFromSchema(schema)
	originalNonBuiltInTypeNames := namesFromDefinitions(originalNonBuiltInTypes)

	addSyntheticTypesForSdkTypesToSchema(schema)

	err := addQueryRelationFieldsToSchemaForTypes(schema, originalNonBuiltInTypeNames)
	if err != nil {
		return err
	}

	synthesizedInputTypes := addSynthesizedInputTypesToSchemaForTypes(schema, originalNonBuiltInTypes)
	addMutationFieldsForSynthesizedInputTypesToSchema(schema, synthesizedInputTypes)
	addQueryFieldsForSynthesizedInputTypesToSchema(schema, synthesizedInputTypes)

	return nil
}

func nonBuiltInTypesFromSchema(schema *ast.Schema) []*ast.Definition {
	nonBuiltInTypes := make([]*ast.Definition, 0, 0)
	for _, typeDefinition := range schema.Types {
		if !typeDefinition.BuiltIn {
			nonBuiltInTypes = append(nonBuiltInTypes, typeDefinition)
		}
	}
	return nonBuiltInTypes
}

func namesFromDefinitions(definitions []*ast.Definition) []string {
	names := make([]string, 0, len(definitions))
	for _, definition := range definitions {
		names = append(names, definition.Name)
	}
	return names
}

type synthesizedInputTypeInfo struct {
	originalType *ast.Definition
	inputType    *ast.Definition
}

func addSynthesizedInputTypesToSchemaForTypes(schema *ast.Schema, typeDefinitions []*ast.Definition) []synthesizedInputTypeInfo {
	synthesizedInputTypes := make([]synthesizedInputTypeInfo, 0, 0)
	for _, typeDefinition := range typeDefinitions {
		synthesizedInputType := new(ast.Definition)
		*synthesizedInputType = *typeDefinition
		synthesizedInputType.Name = typeDefinition.Name + "_Data"
		synthesizedInputType.Kind = ast.InputObject

		log.Println("Adding input type to schema:", synthesizedInputType.Name)
		schema.Types[synthesizedInputType.Name] = synthesizedInputType

		synthesizedInputTypes = append(synthesizedInputTypes, synthesizedInputTypeInfo{
			originalType: typeDefinition,
			inputType:    synthesizedInputType,
		})
	}

	return synthesizedInputTypes
}

func addMutationFieldsForSynthesizedInputTypesToSchema(schema *ast.Schema, synthesizedInputTypes []synthesizedInputTypeInfo) {
	for _, synthesizedInputType := range synthesizedInputTypes {
		mutationFields := make([]*ast.FieldDefinition, 0, 0)
		mutationFields = append(mutationFields, createInsertMutationField(synthesizedInputType))
		mutationFields = append(mutationFields, createDeleteMutationField(synthesizedInputType))
		mutationFields = append(mutationFields, createUpdateMutationField(synthesizedInputType))

		for _, mutationField := range mutationFields {
			log.Println("Adding mutation field to schema:", mutationField.Name)
			schema.Mutation.Fields = append(schema.Mutation.Fields, mutationField)
		}
	}
}

func addQueryFieldsForSynthesizedInputTypesToSchema(schema *ast.Schema, synthesizedInputTypes []synthesizedInputTypeInfo) {
	for _, synthesizedInputType := range synthesizedInputTypes {
		queryFields := make([]*ast.FieldDefinition, 0, 0)
		queryFields = append(queryFields, createSingularQueryField(synthesizedInputType.originalType))
		queryFields = append(queryFields, createPluralQueryField(synthesizedInputType.originalType))

		for _, queryField := range queryFields {
			log.Println("Adding query field to schema:", queryField.Name)
			schema.Query.Fields = append(schema.Query.Fields, queryField)
		}
	}
}

func addSyntheticTypesForSdkTypesToSchema(schema *ast.Schema) {
	schema.Types["sdk:MutationRef.InsertData"] = &ast.Definition{
		Kind:    ast.Scalar,
		Name:    "sdk:MutationRef.InsertData",
		BuiltIn: true,
	}
	schema.Types["sdk:MutationRef.UpdateData"] = &ast.Definition{
		Kind:    ast.Scalar,
		Name:    "sdk:MutationRef.UpdateData",
		BuiltIn: true,
	}
	schema.Types["sdk:MutationRef.DeleteData"] = &ast.Definition{
		Kind:    ast.Scalar,
		Name:    "sdk:MutationRef.DeleteData",
		BuiltIn: true,
	}
}

func createInsertMutationField(synthesizedInputType synthesizedInputTypeInfo) *ast.FieldDefinition {
	arguments := []*ast.ArgumentDefinition{
		&ast.ArgumentDefinition{
			Name: "data",
			Type: &ast.Type{
				NamedType: synthesizedInputType.inputType.Name,
				NonNull:   false,
			},
		},
	}

	return &ast.FieldDefinition{
		Name:      strings.ToLower(synthesizedInputType.originalType.Name) + "_insert",
		Arguments: arguments,
		Type:      &ast.Type{NamedType: "sdk:MutationRef.InsertData", NonNull: true},
	}
}

func createDeleteMutationField(synthesizedInputType synthesizedInputTypeInfo) *ast.FieldDefinition {
	arguments := []*ast.ArgumentDefinition{
		&ast.ArgumentDefinition{
			Name: "id",
			Type: &ast.Type{
				NamedType: "String",
				Elem:      nil,
				NonNull:   false,
				Position:  nil,
			},
		},
	}

	return &ast.FieldDefinition{
		Name:      strings.ToLower(synthesizedInputType.originalType.Name) + "_delete",
		Arguments: arguments,
		Type:      &ast.Type{NamedType: "sdk:MutationRef.DeleteData", NonNull: false},
	}
}

func createUpdateMutationField(synthesizedInputType synthesizedInputTypeInfo) *ast.FieldDefinition {
	arguments := []*ast.ArgumentDefinition{
		&ast.ArgumentDefinition{
			Name: "id",
			Type: &ast.Type{
				NamedType: "String",
				NonNull:   false,
			},
		},
		&ast.ArgumentDefinition{
			Name: "data",
			Type: &ast.Type{
				NamedType: synthesizedInputType.inputType.Name,
				NonNull:   false,
			},
		},
	}

	return &ast.FieldDefinition{
		Name:      strings.ToLower(synthesizedInputType.originalType.Name) + "_update",
		Arguments: arguments,
		Type:      &ast.Type{NamedType: "sdk:MutationRef.UpdateData", NonNull: true},
	}
}

func createQueryFieldArguments(definition *ast.Definition) []*ast.ArgumentDefinition {
	arguments := make([]*ast.ArgumentDefinition, 0, 0)
	for _, field := range definition.Fields {
		argumentType := new(ast.Type)
		*argumentType = *field.Type
		argumentType.NonNull = false
		arguments = append(arguments, &ast.ArgumentDefinition{
			Description: field.Description,
			Name:        field.Name,
			Type:        argumentType,
		})
	}
	return arguments
}

func createSingularQueryField(definition *ast.Definition) *ast.FieldDefinition {
	arguments := createQueryFieldArguments(definition)
	return &ast.FieldDefinition{
		Name:      strings.ToLower(definition.Name),
		Arguments: arguments,
		Type:      &ast.Type{NamedType: definition.Name, NonNull: true},
	}
}

func createPluralQueryField(definition *ast.Definition) *ast.FieldDefinition {
	arguments := createQueryFieldArguments(definition)

	return &ast.FieldDefinition{
		Name:      pluralize.NewClient().Plural(strings.ToLower(definition.Name)),
		Arguments: arguments,
		Type:      &ast.Type{Elem: &ast.Type{NamedType: definition.Name, NonNull: true}, NonNull: true},
	}
}

func addQueryRelationFieldsToSchemaForTypes(schema *ast.Schema, typeNamesToUpdate []string) error {
	type DefinitionFieldDefinitionPair struct {
		definition      *ast.Definition
		fieldDefinition *ast.FieldDefinition
	}
	queryRelationFields := make([]DefinitionFieldDefinitionPair, 0, 0)

	for _, typeNameToUpdate := range typeNamesToUpdate {
		typeDefinition := schema.Types[typeNameToUpdate]
		if typeDefinition == nil {
			return errors.New("schema.Types is missing type specified in typeNamesToUpdate argument: \"" + typeNameToUpdate + "\"")
		}

		for _, fieldDefinition := range typeDefinition.Fields {
			if fieldDefinition.Type.Elem != nil {
				continue // TODO: support lists
			}

			fieldType := schema.Types[fieldDefinition.Type.NamedType]
			if fieldType == nil {
				return errors.New("schema.Types is missing type defined by field of type \"" + typeDefinition.Name + "\": \"" + fieldDefinition.Name + "\"")
			}

			if fieldType.BuiltIn {
				continue
			}

			queryFieldName := pluralize.NewClient().Plural(strings.ToLower(typeDefinition.Name)) +
				"_as_" + strings.ToLower(fieldType.Name)
			queryRelationFields = append(queryRelationFields, DefinitionFieldDefinitionPair{
				definition: fieldType,
				fieldDefinition: &ast.FieldDefinition{
					Name: queryFieldName,
					Type: &ast.Type{
						Elem: &ast.Type{
							NamedType: typeDefinition.Name,
							NonNull:   true,
						},
						NonNull: true,
					},
				},
			})
		}

	}

	// Add the fields after processing all of the given types to avoid generating fields for the
	// generated fields themselves.
	for _, queryRelationField := range queryRelationFields {
		log.Println("Adding query field to type \"" + queryRelationField.definition.Name + "\": " + queryRelationField.fieldDefinition.Name)
		queryRelationField.definition.Fields = append(queryRelationField.definition.Fields, queryRelationField.fieldDefinition)
	}

	return nil
}

func LoadOperationsFile(file string, schema *ast.Schema) (*ast.QueryDocument, error) {
	log.Println("Loading GraphQL operations file:", file)
	source, err := loadGraphQLSourceFromFile(file)
	if err != nil {
		return nil, err
	}

	query, err := parser.ParseQuery(source)
	if err != nil {
		gqlErr, ok := err.(*gqlerror.Error)
		if ok {
			return nil, gqlerror.List{gqlErr}
		}
		return nil, gqlerror.List{gqlerror.Wrap(err)}
	}

	errs := validator.Validate(schema, query)
	if len(errs) > 0 {
		return nil, errs
	}

	return query, nil
}

func loadGraphQLSourceFromFile(file string) (*ast.Source, error) {
	fileBytes, err := os.ReadFile(file)
	if err != nil {
		return nil, err
	}
	return &ast.Source{Name: file, Input: string(fileBytes), BuiltIn: false}, nil
}