-injars ../plain/rudp_plain.jar
-outjars ../obfuscated/ddpie-rudp-1.0.jar

-libraryjars <java.home>/lib/rt.jar
-libraryjars <java.home>/lib/jsse.jar
-libraryjars <java.home>/lib/jce.jar
-libraryjars ../lib/log4j-1.2.15.jar
-libraryjars ../lib/netty-3.4.5.Final.jar

-dontshrink
-dontoptimize
-dontpreverify

-target 1.6
-flattenpackagehierarchy
-obfuscationdictionary chars.txt
-classobfuscationdictionary chars.txt
-packageobfuscationdictionary chars.txt

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep class com.ddpie.rudp.attribute.IAttributeContainer {public protected *;}
-keep class com.ddpie.rudp.channelhandler.FloodAttackHandler {public protected *;}
-keep class com.ddpie.rudp.channelhandler.RudpNettyHandler {public protected *;}
-keep class com.ddpie.rudp.client.RudpClientConnector {public protected *;}
-keep class com.ddpie.rudp.channelhandler.RudpNettyHandler {public protected *;}
-keep class com.ddpie.rudp.config.IRudpConfig {public protected *;}
-keep class com.ddpie.rudp.config.RudpClientConfig {public protected *;}
-keep class com.ddpie.rudp.config.RudpDefaultConfig {public protected *;}
-keep class com.ddpie.rudp.constant.RudpConstants {public protected *;}
-keep class com.ddpie.rudp.filter.IRudpFilter {public protected *;}
-keep class com.ddpie.rudp.filter.RudpFilterAdapter {public protected *;}
-keep class com.ddpie.rudp.filter.codec.RudpCodecFilter {public protected *;}
-keep class com.ddpie.rudp.filter.codec.string.StringCodecFilter {public protected *;}
-keep class com.ddpie.rudp.session.IRudpSession {public protected *;}
-keep class com.ddpie.rudp.session.IRudpSessionManager {public protected *;}

-keepclassmembers enum  ** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keeppackagenames 