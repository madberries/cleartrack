/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class pac_util_FileStat */

#ifndef _Included_pac_util_FileStat
#define _Included_pac_util_FileStat
#ifdef __cplusplus
extern "C" {
#endif
#undef pac_util_FileStat_S_IFMT
#define pac_util_FileStat_S_IFMT 61440L
#undef pac_util_FileStat_S_IFIFO
#define pac_util_FileStat_S_IFIFO 4096L
#undef pac_util_FileStat_S_IFCHR
#define pac_util_FileStat_S_IFCHR 8192L
#undef pac_util_FileStat_S_IFDIR
#define pac_util_FileStat_S_IFDIR 16384L
#undef pac_util_FileStat_S_IFBLK
#define pac_util_FileStat_S_IFBLK 24576L
#undef pac_util_FileStat_S_IFREG
#define pac_util_FileStat_S_IFREG 32768L
#undef pac_util_FileStat_S_IFLNK
#define pac_util_FileStat_S_IFLNK 40960L
#undef pac_util_FileStat_S_IFSOCK
#define pac_util_FileStat_S_IFSOCK 49152L
#undef pac_util_FileStat_S_IFWHT
#define pac_util_FileStat_S_IFWHT 57344L
#undef pac_util_FileStat_S_ISUID
#define pac_util_FileStat_S_ISUID 2048L
#undef pac_util_FileStat_S_ISGID
#define pac_util_FileStat_S_ISGID 1024L
#undef pac_util_FileStat_S_ISVTX
#define pac_util_FileStat_S_ISVTX 512L
#undef pac_util_FileStat_S_IRUSR
#define pac_util_FileStat_S_IRUSR 256L
#undef pac_util_FileStat_S_IWUSR
#define pac_util_FileStat_S_IWUSR 128L
#undef pac_util_FileStat_S_IXUSR
#define pac_util_FileStat_S_IXUSR 64L
/*
 * Class:     pac_util_FileStat
 * Method:    fstat
 * Signature: (Ljava/io/FileDescriptor;)Lpac/util/FileStat;
 */
JNIEXPORT jobject JNICALL Java_pac_util_FileStat_fstat0
  (JNIEnv *, jclass, jobject);

/*
 * Class:     pac_util_FileStat
 * Method:    lstat
 * Signature: (Ljava/lang/String;)Lpac/util/FileStat;
 */
JNIEXPORT jobject JNICALL Java_pac_util_FileStat_lstat0
  (JNIEnv *, jclass, jstring);

/*
 * Class:     pac_util_FileStat
 * Method:    stat
 * Signature: (Ljava/lang/String;)Lpac/util/FileStat;
 */
JNIEXPORT jobject JNICALL Java_pac_util_FileStat_stat0
  (JNIEnv *, jclass, jstring);

/*
 * Class:     pac_util_FileStat
 * Method:    getMimetype0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_pac_util_FileStat_getMimetype0
  (JNIEnv *, jclass, jstring);

/*
 * Class:     pac_util_FileStat
 * Method:    realpath0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */

JNIEXPORT jstring JNICALL Java_pac_util_FileStat_realpath0
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
