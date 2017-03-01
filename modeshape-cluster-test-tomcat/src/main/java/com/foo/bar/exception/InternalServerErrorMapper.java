package com.foo.bar.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class InternalServerErrorMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalServerErrorMapper.class);
    
    @Override
    public Response toResponse(Exception exception) {
        LOGGER.error("An unexpected error occurred", exception);
        
        return Response.ok(exception.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
    }

}
