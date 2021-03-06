package com.chat_maps.chatmaps.managers;

/**
 * Created by Rafael on 2/21/2016.
 */
public interface IQueryListener<T> {
    public void handleResults(TypedCursor<T> results);
    public void closeResults();
}
