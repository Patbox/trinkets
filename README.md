# Trinkets (Updated)
A data-driven accessory mod for Minecraft for Fabric and NeoForge modloaders, based on
[Trinkets by Emi](https://modrinth.com/mod/trinkets)

![Image of the Trinkets UI](https://user-images.githubusercontent.com/14813658/221322531-2ddb822f-531c-44b2-84c7-bef8b8064b55.png)

## About
Trinkets adds a slot group and slot system to Minecraft. Slot groups are collections of slots for a certain body part or more vague area. By default there are 6 slot groups (head, chest, legs, feet, offhand, hand) that can have slots added to them, but more groups can be added if desired. Trinkets' UI is intuitive to use, accessible, and attempts to do away with clutter. Its system means that you'll never have a slot that's not used for anything, as mods request the slots they want.

## Download
You can get this mod from [Modrinth](https://modrinth.com/mod/trinkets-updated), 
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/trinkets-updated)
or [GitHub Releases](https://github.com/Patbox/trinkets/releases)

## Developers
To add Trinkets to your project you need to add these repos to your repositories in your build.gradle
```gradle
repositories {
	maven {
		name = "Nucleoid"
		url = 'https://maven.nucleoid.xyz/releases'
	}
}
```
And then to add Trinkets you add it as a dependency in your build.gradle
```gradle
dependencies {
	implementation "eu.pb4:trinkets:${trinkets_version}"
}
```
Trinkets works as a universal jar, which means the same jar file can be used for Fabric, NeoForge and common.
For optional / conditional compatibility, you can check for `trinkets_updated` mod id on both platform.

For versions, see <https://maven.nucleoid.xyz/#/releases/eu/pb4/trinkets>

For basic tutorials and comprehensive documentation, visit this repository's [wiki](https://github.com/patbox/trinkets/wiki/Home)!
You can also see the test mod here: https://github.com/Patbox/trinkets/tree/main/src/testmod
