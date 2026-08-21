package com.prisma.exception;

public class PrismaException extends RuntimeException {
    
    public PrismaException(String message) {
        super(message);
    }

    public PrismaException(String message, Throwable cause) {
        super(message, cause);
    }
}
