package fr.pchab.androidrtc.base;

import fr.pchab.androidrtc.base.BasePresenterView;

/**
 * Created by Seokjoo on 2016-07-14.
 */
public class BasePresenter  <T extends BasePresenterView> {

    private T view;

    public BasePresenter(T view) {
        this.view = view;
    }

    protected T getView() {
        return view;
    }
}
