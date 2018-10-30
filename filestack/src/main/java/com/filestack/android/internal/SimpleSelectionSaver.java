package com.filestack.android.internal;

import com.filestack.android.Selection;

import java.util.ArrayList;

/**
 * Implementation of {{@link SelectionSaver}}. Just saves selections to an {{@link ArrayList}}.
 */
public class SimpleSelectionSaver implements SelectionSaver {
    private ArrayList<Selection> selections = new ArrayList<>();
    private Listener listener;

    @Override
    public void setItemChangeListener(Listener listener) {
        if (listener == null) {
            return;
        }
        this.listener = listener;
        this.listener.onEmptyChanged(isEmpty());
    }

    @Override
    public ArrayList<Selection> getItems() {
        return selections;
    }

    @Override
    public void clear() {
        if (selections.size() != 0) {
            selections.clear();
            if (listener != null) {
                listener.onEmptyChanged(true);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return selections.size() == 0;
    }

    @Override
    public boolean toggleItem(Selection item) {
        boolean isSaved;

        boolean wasEmpty = isEmpty();

        if (isSelected(item)) {
            selections.remove(item);
            isSaved = false;
        } else {
            selections.add(item);
            isSaved = true;
        }

        boolean isEmpty = isEmpty();

        if (listener != null && wasEmpty != isEmpty) {
            listener.onEmptyChanged(isEmpty);
        }

        return isSaved;
    }

    @Override
    public boolean isSelected(Selection item) {
        return selections.contains(item);
    }
}
