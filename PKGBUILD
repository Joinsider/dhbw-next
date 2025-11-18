# Maintainer: DHBW Next Team <https://github.com/Joinsider/dhbw-next>
pkgname=dhbw-next
pkgver=1.0.4
pkgrel=1
pkgdesc="DHBW Horb Studenten App - A Kotlin Multiplatform application for DHBW students"
arch=('x86_64')
url="https://github.com/Joinsider/dhbw-next"
license=('custom')
depends=('java-runtime>=11')
makedepends=('jdk11-openjdk' 'gradle')
source=("${pkgname}-${pkgver}.tar.gz::https://github.com/Joinsider/dhbw-next/archive/refs/tags/v${pkgver}.tar.gz")
sha256sums=('SKIP')

build() {
    cd "${srcdir}/${pkgname}-${pkgver}"
    
    # Set JAVA_HOME to use the correct JDK
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
    
    # Build the desktop application
    ./gradlew :composeApp:createDistributable --no-daemon
}

package() {
    cd "${srcdir}/${pkgname}-${pkgver}"
    
    # Install the application
    install -dm755 "${pkgdir}/opt/${pkgname}"
    
    # Find and copy the built application files
    # The Compose Desktop plugin creates a distribution in composeApp/build/compose/binaries/main/
    cp -r composeApp/build/compose/binaries/main/app/* "${pkgdir}/opt/${pkgname}/"
    
    # Create a launcher script
    install -dm755 "${pkgdir}/usr/bin"
    cat > "${pkgdir}/usr/bin/${pkgname}" << EOF
#!/bin/bash
/opt/${pkgname}/bin/DHBW\ Horb\ Studenten\ App "\$@"
EOF
    chmod +x "${pkgdir}/usr/bin/${pkgname}"
    
    # Install icon
    install -Dm644 composeApp/icon.png "${pkgdir}/usr/share/pixmaps/${pkgname}.png"
    
    # Create desktop entry
    install -dm755 "${pkgdir}/usr/share/applications"
    cat > "${pkgdir}/usr/share/applications/${pkgname}.desktop" << EOF
[Desktop Entry]
Type=Application
Name=DHBW Horb Studenten App
Comment=${pkgdesc}
Exec=${pkgname}
Icon=${pkgname}
Categories=Education;
Terminal=false
EOF
}
