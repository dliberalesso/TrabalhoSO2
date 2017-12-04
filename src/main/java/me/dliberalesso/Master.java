package me.dliberalesso;

public interface Master {
    Object submit(Task task, long timeout) throws Exception;
}