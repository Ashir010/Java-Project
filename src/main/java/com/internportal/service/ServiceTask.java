package com.internportal.service;

/** A piece of work that runs on a background thread and may fail with a ServiceException. */
@FunctionalInterface
public interface ServiceTask {
    void run() throws ServiceException;
}
