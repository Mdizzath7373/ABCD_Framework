package com.eit.abcdframework.globalhandler;

public enum PostgreSQLErrorCode {
    SYNTAX_ERROR("42601", "Syntax error"),
    INVALID_NAME("42602", "Invalid name syntax"),
    UNDEFINED_TABLE("42P01", "Relation/table does not exist"),
    UNDEFINED_COLUMN("42703", "Column does not exist"),
    DUPLICATE_TABLE("42P07", "Table already exists"),
    DUPLICATE_COLUMN("42701", "Column already exists"),
    UNDEFINED_FUNCTION("42883", "Function does not exist"),
    DATATYPE_MISMATCH("42804", "Column type mismatch"),
    UNDEFINED_OBJECT("42704", "Referenced object does not exist"),
    UNDEFINED_FUNCTION_PARAM("42883", "Function does not exist with specified name and argument types"),
    DUPLICATE_OBJECT("42710", "Duplicate object"),
    MISSING_FROM_CLAUSE("42P02", "Missing FROM-clause entry for table"),

    PGRST21000("21000", "update command cannot affect row a second time"),
    NUMERIC_VALUE_OUT_OF_RANGE("22003", "Numeric value out of range"),
    INVALID_TEXT_REPRESENTATION("22P02", "Invalid text representation"),
    INVALID_DATETIME_FORMAT("22007", "Invalid datetime format"),
    STRING_DATA_TOO_LONG("22001", "String data too long for column"),
    NULL_VALUE_NOT_ALLOWED("22004", "Null value not allowed in column"),
 
    FOREIGN_KEY_VIOLATION("23503", "Foreign key violation"),
    UNIQUE_VIOLATION("23505", "Unique constraint violation"),
    CHECK_VIOLATION("23514", "Check constraint violation"),
    NOT_NULL_VIOLATION("23502", "Not null violation"),
        
    INSUFFICIENT_PRIVILEGE("42501", "Insufficient privilege"),
    INVALID_PASSWORD("28P01", "Invalid password/authentication failure"),
    
    // Group 1 - Request Errors
    PGRST100("PGRST100", "Parsing error in the query string parameter."),
    PGRST101("PGRST101", "Only GET and POST verbs are allowed for functions."),
    PGRST102("PGRST102", "Invalid request body sent (e.g., empty or malformed JSON)."),
    PGRST103("PGRST103", "Invalid range specified for limits and pagination."),
    PGRST105("PGRST105", "Invalid PUT request."),
    PGRST106("PGRST106", "Schema specified does not exist in db-schemas config."),
    PGRST107("PGRST107", "Invalid Content-Type in request."),
    PGRST108("PGRST108", "Filter applied to an embedded resource not selected."),
    PGRST111("PGRST111", "Invalid response.headers set."),
    PGRST112("PGRST112", "Status code must be a positive integer."),
    PGRST114("PGRST114", "UPSERT with PUT cannot use limits or offsets."),
    PGRST115("PGRST115", "UPSERT with PUT has mismatched primary key in query and body."),
    PGRST116("PGRST116", "More than 1 or no items returned when singular response expected."),
    PGRST117("PGRST117", "HTTP verb used is not supported."),
    PGRST118("PGRST118", "No valid relationship for ordering by related table."),
    PGRST120("PGRST120", "Embedded resource can only be filtered with is.null or not.is.null."),
    PGRST121("PGRST121", "Cannot parse JSON objects in RAISE PGRST error."),
    PGRST122("PGRST122", "Invalid preferences in Prefer header with strict handling."),
    PGRST123("PGRST123", "Aggregate functions are disabled."),
    PGRST124("PGRST124", "max-affected preference violated."),
    PGRST125("PGRST125", "Invalid path in request URL."),
    PGRST126("PGRST126", "API root accessed but OpenAPI config is disabled."),
    PGRST127("PGRST127", "Feature in details field is not implemented."),

    // Group 2 - Schema Cache
    PGRST200("PGRST200", "Stale foreign key relationship or missing embedding resource."),
    PGRST201("PGRST201", "Ambiguous embedding request."),
    PGRST202("PGRST202", "Stale function signature or missing function."),
    PGRST203("PGRST203", "Overloaded functions with same names cause ambiguity."),
    PGRST204("PGRST204", "Column specified in query parameter not found."),
    PGRST205("PGRST205", "Table specified in URI not found."),

    // Group 3 - JWT Errors
    PGRST300("PGRST300", "Missing JWT secret in configuration."),
    PGRST301("PGRST301", "JWT is invalid or cannot be decoded."),
    PGRST302("PGRST302", "Authentication attempted without anonymous role."),
    PGRST303("PGRST303", "JWT claims validation or parsing failed."),
    
 // Optional custom PostgREST-style errors (can be user-defined)
    PGRST400("PGRST400", "Request timeout or gateway timeout"),
    PGRST401("PGRST401", "Unauthorized access – invalid JWT token"),
    PGRST402("PGRST402", "Payment required"),
    PGRST404("PGRST404", "Resource not found"),
    PGRST409("PGRST409", "Conflict – duplicate insert or row conflict"),
    PGRST413("PGRST413", "Payload too large"),
    PGRST429("PGRST429", "Too many requests – rate limited"),

    // Group X - Internal Errors
    PGRSTX00("PGRSTX00", "Internal PostgREST error. Please report this bug."),

    UNKNOWN_ERROR("XXXXX", "Unknown error");
    
    private final String code;
    private final String description;
    
    PostgreSQLErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static String getDescriptionByCode(String code) {
        if (code == null) {
            return UNKNOWN_ERROR.getDescription();
        }

        for (PostgreSQLErrorCode error : PostgreSQLErrorCode.values()) {
            if (error.getCode().equals(code)) {
                return error.getDescription();
            }
        }

        return UNKNOWN_ERROR.getDescription();
    }

}
