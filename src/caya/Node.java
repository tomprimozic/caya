package caya;

import java.math.BigInteger;
import java.util.regex.Pattern;

import caya.Parser.Location;

public interface Node {
    final static Pattern remove_loc = Pattern.compile("loc=Pos\\[pos=\\d+\\](\\-Pos\\[pos=\\d+\\])?, ");
    final static Pattern remove_field_names = Pattern.compile("(?<=\\[|, )[_a-z]+=");
    public static String show(Node n) {
        var s = remove_loc.matcher(n.toString()).replaceAll("");
        s = remove_field_names.matcher(s).replaceAll("");
        return s;
    }

    record Err(Location loc, String error) implements Node {}
    record Int(Location loc, BigInteger value) implements Node {}
    record Bool(Location loc, boolean value) implements Node {}
    record None(Location loc) implements Node {}
    record Field(Location loc, Node expr, String field) implements Node {}
}
