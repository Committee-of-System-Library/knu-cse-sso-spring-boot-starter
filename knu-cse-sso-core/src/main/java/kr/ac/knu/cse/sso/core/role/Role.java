package kr.ac.knu.cse.sso.core.role;

public enum Role {
    ADMIN(3),
    EXECUTIVE(2),
    FINANCE(2),
    PLANNING(2),
    PR(2),
    CULTURE(2),
    STUDENT(1);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherOrEqual(Role required) {
        return this.level >= required.level;
    }
}
