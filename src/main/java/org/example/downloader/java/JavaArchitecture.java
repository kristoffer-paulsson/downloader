package org.example.downloader.java;

public enum JavaArchitecture {
    PPC64("ppc64"),
    PPC64LE("ppc64le"),
    RISCV64("riscv64"),
    S390X("s390x"),
    SPARC64("sparc64"),
    X64("x64"),
    AARCH64("aarch64");

    private final String arch;

    JavaArchitecture(String arch) {
        this.arch = arch;
    }

    public String getArch() {
        return arch;
    }
}
