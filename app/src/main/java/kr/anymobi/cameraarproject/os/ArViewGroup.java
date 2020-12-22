package kr.anymobi.cameraarproject.os;

import java.util.Vector;

/**
 * <code>ArViewGroup</code>은 다른 <code>ArView</code>들을 보유할수 있는 특별한 뷰이다.
 */
public class ArViewGroup extends ArView {

    protected Vector<ArView> mChildren;

    public ArViewGroup() {
        mChildren = new Vector<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void dispatchDestroy() {
        super.dispatchDestroy();
        for (ArView view : mChildren) {
            view.destroy();
        }
    }

    /**
     * 자식뷰들의 count를 가져온다.
     * @return 자식뷰들의 count
     */
    public int getChildCount() {
        return mChildren.size();
    }

    /**
     * index에 존재하는 자식뷰를 가져온다.
     * @param index
     * @return
     */
    public ArView getViewAt(int index) {
        if (index < 0 || index >= mChildren.size())
            return null;
        return mChildren.get(index);
    }

    /**
     * 자식뷰를 추가한다.
     * @param view
     */
    public void addView(ArView view) {
        mChildren.add(view);
        view.setParent(this);
    }

    /**
     * 자식뷰를 제거한다.
     * @param view
     */
    public void removeView(ArView view) {
        view.destroy();
        mChildren.remove(view);
    }

    /**
     * 자식뷰들 모두를 제거한다.
     */
    public void removeAllViews() {
        for (ArView view : mChildren) {
            view.destroy();
        }
        mChildren.clear();
    }

    /**
     * {@inheritDoc}
     */
    protected ArView findViewByIdTraversal(int id) {
        ArView v = super.findViewByIdTraversal(id);
        if (v == null) {
            for (ArView view : mChildren) {
                v = view.findViewById(id);
                if (v != null) {
                    break;
                }
            }
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    protected ArView findViewByPickIdTraversal(int id) {
        ArView v = super.findViewByPickIdTraversal(id);
        if (v == null) {
            for (ArView view : mChildren) {
                v = view.findViewByPickId(id);
                if (v != null) {
                    break;
                }
            }
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void dispatchDraw(ArCanvas arCanvas) {
        for (ArView view : mChildren) {
            view.draw(arCanvas);
        }
    }
}