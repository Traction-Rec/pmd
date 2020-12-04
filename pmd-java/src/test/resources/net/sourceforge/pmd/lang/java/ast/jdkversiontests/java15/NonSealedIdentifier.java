/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

public class NonSealedIdentifier {
    public static void main(String[] args) {
        int result = 0;
        int non = 1;
        // sealed is a valid identifier name in both Java15 and Java15 Preview
        int sealed = 2;
        // non-sealed is a valid subtraction expression in both Java15 and Java15 Preview
        result = non-sealed;
        System.out.println(result);
    }
}
