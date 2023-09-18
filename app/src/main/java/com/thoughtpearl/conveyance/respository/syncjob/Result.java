package com.thoughtpearl.conveyance.respository.syncjob;

public abstract class Result<T> {
    private Result() {}

    public static final class Success<T> extends Result<T> {
        public  T data;
        public Success(T data) {
            this.data = data;
        }
    }

    public static final class Failure<T> extends Result<T> {
        public Throwable  exception;
        public Failure(Throwable exception) {
            this.exception = exception;
        }
    }
}
