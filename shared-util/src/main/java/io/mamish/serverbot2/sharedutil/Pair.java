package io.mamish.serverbot2.sharedutil;

public class Pair<A,B> {

    private A a;
    private B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A fst() {
        return a;
    }

    public B snd() {
        return b;
    }
}
