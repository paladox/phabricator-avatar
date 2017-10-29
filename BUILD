load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

gerrit_plugin(
    name = "phabricator-avatar",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-AvatarProvider: com.googlesource.gerrit.plugins.phabricator.avatars.PhabricatorUrlAvatarProvider",
        "Implementation-Title: Plugin phabricator-avatar",
        "Implementation-Vendor: Wikimedia Foundation",
        "Implementation-URL: https://gerrit.googlesource.com/plugins/phabricator-avatar",
    ],
    resources = glob(["src/main/**/*"]),
)

