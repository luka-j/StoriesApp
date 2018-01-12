package rs.lukaj.android.stories.controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

/**
 * Maintains history of the state. Used by StoryEditorActivity when going backwards
 * to determine which was the previous assignment (without actually iterating back
 * to the beginning). Expressions is the default generic parameter, though any could
 * be used (and it has changed multiple times already). Throughout this class, the
 * position of the ListIterator is usually referred to as 'the pointer'. There is
 * one ListIterator per variable.
 * Created by luka on 12.1.18.
 */
//this class definitely has highest time invested/LoC ratio
public class StateHistory<T> {
    //maintains assignment history for every variable
    //the current state is the one in front of the pointer, i.e. obtained by a call to next()
    private Map<String, ListIterator<T>> history = new HashMap<>();

    public StateHistory() {

    }

    /**
     * Inserts a new record after current position and positions the pointer right before it.
     * @param variable name of the variable to which the record refers to
     * @param newValue value to insert to the record
     */
    public void update(String variable, T newValue) {
        stepForward(variable); //ignored if record doesn't exist already
        if(history.containsKey(variable)) {
            history.get(variable).add(newValue);
        } else {
            //we're using only ListIterators, don't need the List itself
            ListIterator<T> it = new LinkedList<T>().listIterator();
            it.add(null); //this is the 'default' - need it because by default variables are undeclared
            it.add(newValue);
            history.put(variable, it);
        }
        stepBackward(variable);
    }

    /**
     * Move the pointer one step forward, if variable exists and pointer can move forward
     * @param variable which pointer
     * @return true if the operation is successful, false otherwise
     */
    public boolean stepForward(String variable) {
        if(!history.containsKey(variable)) return false;
        if(!history.get(variable).hasNext()) return false;
        history.get(variable).next();
        return true;
    }

    /**
     * Move the pointer one step backward, if variable exists and pointer can move backward
     * @param variable which pointer
     * @return true if the operation is successful, false otherwise
     */
    public boolean stepBackward(String variable) {
        if(!history.containsKey(variable)) return false;
        if(!history.get(variable).hasPrevious()) return false;
        history.get(variable).previous();
        return true;
    }

    /**
     * Return next (i.e. current) element, without moving pointer
     * @param variable which pointer
     * @return the element, if it exists, false otherwise
     */
    public T peek(String variable) {
        if(stepForward(variable))
            return history.get(variable).previous();
        else return null; //shouldn't happen if used in conjunction with stepBackward
    }
}
