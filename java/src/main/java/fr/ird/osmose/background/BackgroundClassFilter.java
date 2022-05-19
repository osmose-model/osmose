package fr.ird.osmose.background;

import fr.ird.osmose.util.filter.IFilter;

public class BackgroundClassFilter implements IFilter<BackgroundSchool> {

    final private int iClass;

    public BackgroundClassFilter(int iClass) {
        this.iClass = iClass;
    }

    @Override
    public boolean accept(BackgroundSchool school) {
        return iClass == school.getClassIndex();
    }

}