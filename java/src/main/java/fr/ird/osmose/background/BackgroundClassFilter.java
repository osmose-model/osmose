package fr.ird.osmose.background;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.util.filter.IFilter;

public class BackgroundClassFilter implements IFilter<AbstractSchool> {

    final private int iClass;

    public BackgroundClassFilter(int iClass) {
        this.iClass = iClass;
    }

    @Override
    public boolean accept(AbstractSchool school) {
        return iClass == school.getClassIndex();
    }

}