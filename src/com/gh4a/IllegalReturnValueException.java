package com.gh4a;

/**
 * Thrown to indicate that a method has returned an illegal or
 * inappropriate value.
 */
public class IllegalReturnValueException extends RuntimeException {
    /**
     * Constructs an <code>IllegalReturnValueException</code> with no
     * detail message.
     */
    public IllegalReturnValueException() {
        super();
    }

    /**
     * Constructs an <code>IllegalReturnValueException</code> with the
     * specified detail message.
     *
     * @param message the detail message.
     */
    public IllegalReturnValueException(String message) {
        super(message);
    }
}
