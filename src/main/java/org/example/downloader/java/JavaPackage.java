package org.example.downloader.java;

public enum JavaPackage {
    DEB("deb"),
    RPM("rpm"),
    ZIP("zip"),
    MSI("msi"),
    EXE("exe"),
    TAR_GZ("tar.gz"),
    DMG("dmg");

    private final String packageType;

    JavaPackage(String packageType) {
        this.packageType = packageType;
    }

    public String getPackageType() {
        return packageType;
    }
}
