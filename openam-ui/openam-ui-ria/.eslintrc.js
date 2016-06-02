module.exports = {
    root: true,
    extends: [
        "forgerock"
    ],
    parser: "babel-eslint",
    parserOptions: {
        ecmaVersion: 6
    },
    env: {
        amd: true,
        browser: true
    },
    rules: {
        /*
         * --------------------------------------------------------------------------------
         * ERROR RULES
         *
         * These are rules we're sure about. They will cause the build to fail.
         * --------------------------------------------------------------------------------
         */
        "array-bracket-spacing": [2, "never"],
        "arrow-parens": [2, "always"],
        "arrow-spacing": 2,
        "block-spacing": [2, "always"],
        "brace-style": [2, "1tbs", { "allowSingleLine": true }],
        "camelcase": [2, {
            "properties": "always"
        }],
        "comma-spacing": [2, {
            "before": false,
            "after": true
        }],
        "comma-style": 2,
        "constructor-super": 2,
        "dot-location": [2, "property"],
        "eol-last": 2,
        "guard-for-in": 2,
        "indent": [2, 4, {
            "SwitchCase": 1,
            "VariableDeclarator": 1
        }],
        "keyword-spacing": 2,
        "max-len": [2, 120, 4, {
            "ignoreComments": true
        }],
        "new-cap": [2, {
            "capIsNew": false
        }],
        "new-parens": 2,
        "no-alert": 2,
        "no-bitwise": 2,
        "no-catch-shadow": 2,
        "no-class-assign": 2,
        "no-confusing-arrow": 2,
        "no-constant-condition": 2,
        "no-continue": 2,
        "no-dupe-class-members": 2,
        "no-duplicate-case": 2,
        "no-empty-character-class": 2,
        "no-empty-pattern": 2,
        "no-extend-native": 2,
        "no-implicit-globals": 2,
        "no-invalid-regexp": 2,
        "no-irregular-whitespace": 2,
        "no-labels": 2,
        "no-lonely-if": 2,
        "no-mixed-spaces-and-tabs": 2,
        "no-multiple-empty-lines": 2,
        "no-multi-spaces": 2,
        "no-multi-str": 2,
        "no-native-reassign": 2,
        "no-self-assign": 2,
        "no-trailing-spaces": 2,
        "no-unmodified-loop-condition": 2,
        "no-unused-vars": 2,
        "no-useless-escape": 2,
        "no-void": 2,
        "no-whitespace-before-property": 2,
        "object-curly-spacing": [2, "always"],
        "object-shorthand": 2,
        "operator-linebreak": 2,
        "prefer-const": 2,
        "prefer-template": 2,
        "quotes": [2, "double", "avoid-escape"],
        "semi-spacing": [2, {
            "before": false,
            "after": true
        }],
        "space-before-blocks": [2, "always"],
        "space-before-function-paren": [2, "always"],
        "space-in-parens": [2, "never"],
        "space-infix-ops": [2, {
            "int32Hint": false
        }],
        "space-unary-ops": 2,
        "template-curly-spacing": 2,
        "valid-jsdoc": [2, {
            "prefer": {
                "return": "returns"
            },
            "requireReturn": false
        }],
        "yoda": [2, "never"],

        /*
         * --------------------------------------------------------------------------------
         * WARNING RULES
         *
         * These are rules that we want to turn into errors but can't yet because there are
         * too many violations. As we fix the violations, we will transition them into
         * error rules.
         * --------------------------------------------------------------------------------
         */
        "no-var": 1,
        "prefer-arrow-callback": 1,
        "prefer-spread": 1,

        // TODO: Need an abstraction for logging before we can enable this.
        //"no-console": 0
        //"no-param-reassign": 0

        /**
         * Disabled rules
         */
        "arrow-body-style": 0,
        "sort-imports": 0,

        /**
         * Disabled because these rules aren't available in ESLint 2.0.
         * TODO: Remove them from eslint-config-forgerock
         */
        "no-empty-label": 0,
        "space-return-throw-case": 0
    }
};
