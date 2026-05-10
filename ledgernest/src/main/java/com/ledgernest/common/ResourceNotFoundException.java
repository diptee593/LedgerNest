package com.ledgernest.common;

// This is thrown when requested source/data is not exist in ledgernest

public class ResourceNotFoundException extends RuntimeException{
     public ResourceNotFoundException(String message) {
        super(message);
    }

    // Convenience constructor: ResourceNotFoundException("Invoice", id)
    // Produces: "Invoice not found with id: <uuid>"
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }
    
}