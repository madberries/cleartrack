#include <jni.h>
#include <unistd.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <magic.h>
#include "pac_util_FileStat.h"

#ifdef __APPLE_CC__
#define CREATE_STAT(env, statObj, c, fileStat) { \
  jfieldID modeId = (*env)->GetFieldID(env, c, "mode", "I"); \
  (*env)->SetIntField(env, statObj, modeId, fileStat.st_mode); \
  jfieldID uidId = (*env)->GetFieldID(env, c, "uid", "I"); \
  (*env)->SetIntField(env, statObj, uidId, fileStat.st_uid); \
  jfieldID gidId = (*env)->GetFieldID(env, c, "gid", "I"); \
  (*env)->SetIntField(env, statObj, gidId, fileStat.st_gid); \
  jfieldID nlinkId = (*env)->GetFieldID(env, c, "nlink", "I"); \
  (*env)->SetIntField(env, statObj, nlinkId, fileStat.st_nlink); \
  jfieldID devId = (*env)->GetFieldID(env, c, "dev", "J"); \
  (*env)->SetLongField(env, statObj, devId, fileStat.st_dev); \
  jfieldID rdevId = (*env)->GetFieldID(env, c, "rdev", "J"); \
  (*env)->SetLongField(env, statObj, rdevId, fileStat.st_rdev); \
  jfieldID inodeId = (*env)->GetFieldID(env, c, "inode", "J"); \
  (*env)->SetLongField(env, statObj, inodeId, fileStat.st_ino); \
  jfieldID sizeId = (*env)->GetFieldID(env, c, "size", "J"); \
  (*env)->SetLongField(env, statObj, sizeId, fileStat.st_size); \
  jfieldID atimeId = (*env)->GetFieldID(env, c, "atime", "J"); \
  (*env)->SetLongField(env, statObj, atimeId, fileStat.st_atimespec.tv_sec); \
  jfieldID mtimeId = (*env)->GetFieldID(env, c, "mtime", "J"); \
  (*env)->SetLongField(env, statObj, mtimeId, fileStat.st_mtimespec.tv_sec); \
  jfieldID ctimeId = (*env)->GetFieldID(env, c, "ctime", "J"); \
  (*env)->SetLongField(env, statObj, ctimeId, fileStat.st_ctimespec.tv_sec); \
  jfieldID atimensecId = (*env)->GetFieldID(env, c, "atime_nsec", "J"); \
  (*env)->SetLongField(env, statObj, atimensecId, fileStat.st_atimespec.tv_nsec); \
  jfieldID mtimensecId = (*env)->GetFieldID(env, c, "mtime_nsec", "J"); \
  (*env)->SetLongField(env, statObj, mtimensecId, fileStat.st_mtimespec.tv_nsec); \
  jfieldID ctimensecId = (*env)->GetFieldID(env, c, "ctime_nsec", "J"); \
  (*env)->SetLongField(env, statObj, ctimensecId, fileStat.st_ctimespec.tv_nsec); \
}
#else
#define CREATE_STAT(env, statObj, c, fileStat) { \
  jfieldID modeId = (*env)->GetFieldID(env, c, "mode", "I"); \
  (*env)->SetIntField(env, statObj, modeId, fileStat.st_mode); \
  jfieldID uidId = (*env)->GetFieldID(env, c, "uid", "I"); \
  (*env)->SetIntField(env, statObj, uidId, fileStat.st_uid); \
  jfieldID gidId = (*env)->GetFieldID(env, c, "gid", "I"); \
  (*env)->SetIntField(env, statObj, gidId, fileStat.st_gid); \
  jfieldID nlinkId = (*env)->GetFieldID(env, c, "nlink", "I"); \
  (*env)->SetIntField(env, statObj, nlinkId, fileStat.st_nlink); \
  jfieldID devId = (*env)->GetFieldID(env, c, "dev", "J"); \
  (*env)->SetLongField(env, statObj, devId, fileStat.st_dev); \
  jfieldID rdevId = (*env)->GetFieldID(env, c, "rdev", "J"); \
  (*env)->SetLongField(env, statObj, rdevId, fileStat.st_rdev); \
  jfieldID inodeId = (*env)->GetFieldID(env, c, "inode", "J"); \
  (*env)->SetLongField(env, statObj, inodeId, fileStat.st_ino); \
  jfieldID sizeId = (*env)->GetFieldID(env, c, "size", "J"); \
  (*env)->SetLongField(env, statObj, sizeId, fileStat.st_size); \
  jfieldID atimeId = (*env)->GetFieldID(env, c, "atime", "J"); \
  (*env)->SetLongField(env, statObj, atimeId, fileStat.st_atim.tv_sec); \
  jfieldID mtimeId = (*env)->GetFieldID(env, c, "mtime", "J"); \
  (*env)->SetLongField(env, statObj, mtimeId, fileStat.st_mtim.tv_sec); \
  jfieldID ctimeId = (*env)->GetFieldID(env, c, "ctime", "J"); \
  (*env)->SetLongField(env, statObj, ctimeId, fileStat.st_ctim.tv_sec); \
  jfieldID atimensecId = (*env)->GetFieldID(env, c, "atime_nsec", "J"); \
  (*env)->SetLongField(env, statObj, atimensecId, fileStat.st_atim.tv_nsec); \
  jfieldID mtimensecId = (*env)->GetFieldID(env, c, "mtime_nsec", "J"); \
  (*env)->SetLongField(env, statObj, mtimensecId, fileStat.st_mtim.tv_nsec); \
  jfieldID ctimensecId = (*env)->GetFieldID(env, c, "ctime_nsec", "J"); \
  (*env)->SetLongField(env, statObj, ctimensecId, fileStat.st_ctim.tv_nsec); \
}
#endif

JNIEXPORT jobject JNICALL Java_pac_util_FileStat_fstat0
  (JNIEnv *env, jclass c, jobject fdObj) {
  
  // acquire the actual file descriptor from the FileDescriptor object.
  jclass fdClass = (*env)->GetObjectClass(env, fdObj);
  jfieldID fdId = (*env)->GetFieldID(env, fdClass, "fd", "I");
  jint fd = (*env)->GetIntField(env, fdObj, fdId);
  
  // make fstat system call, and throw an IOException if there is an error
  struct stat fileStat;
  if (fstat(fd, &fileStat) < 0) {
    char exBuffer[sizeof(int)*3+46];
    snprintf(exBuffer, sizeof exBuffer, "Error invoking fstat() on file descriptor = %d", fd);
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), exBuffer);
    return NULL;
  }
  
  // create the new Stat object
  jmethodID constructor = (*env)->GetMethodID(env, c, "<init>", "()V");
  jobject statObj = (*env)->NewObject(env, c, constructor);
  CREATE_STAT(env, statObj, c, fileStat)
  return statObj;
}

JNIEXPORT jobject JNICALL Java_pac_util_FileStat_lstat0
  (JNIEnv *env, jclass c, jstring filename) {
 
  const jbyte* filenameChars = (*env)->GetStringUTFChars(env, filename, NULL);
 
  // make fstat system call, and throw an IOException if there is an error
  struct stat fileStat;
  if (lstat(filenameChars, &fileStat) < 0) {
    char* exBuffer = malloc(sizeof(char)*(strlen(filenameChars)+34));
    sprintf(exBuffer, "Error invoking lstat() on file '%s'", filenameChars);
    (*env)->ReleaseStringUTFChars(env, filename, filenameChars);  // free chars as they are no longer needed.
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), exBuffer);
    free(exBuffer);
    return NULL;
  } else {
    (*env)->ReleaseStringUTFChars(env, filename, filenameChars);  // free chars as they are no longer needed.
  }
  
  // create the new Stat object
  jmethodID constructor = (*env)->GetMethodID(env, c, "<init>", "()V");
  jobject statObj = (*env)->NewObject(env, c, constructor);
  CREATE_STAT(env, statObj, c, fileStat)
  return statObj;
}

JNIEXPORT jobject JNICALL Java_pac_util_FileStat_stat0
  (JNIEnv *env, jclass c, jstring filename) {
 
  const jbyte* filenameChars = (*env)->GetStringUTFChars(env, filename, NULL);
 
  // make fstat system call, and throw an IOException if there is an error
  struct stat fileStat;
  if (stat(filenameChars, &fileStat) < 0) {
    char* exBuffer = malloc(sizeof(char)*(strlen(filenameChars)+33));
    sprintf(exBuffer, "Error invoking stat() on file '%s'", filenameChars);
    (*env)->ReleaseStringUTFChars(env, filename, filenameChars);  // free chars as they are no longer needed.
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), exBuffer);
    free(exBuffer);
    return NULL;
  } else {
    (*env)->ReleaseStringUTFChars(env, filename, filenameChars);  // free chars as they are no longer needed.
  }
  
  // create the new Stat object
  jmethodID constructor = (*env)->GetMethodID(env, c, "<init>", "()V");
  jobject statObj = (*env)->NewObject(env, c, constructor);
  CREATE_STAT(env, statObj, c, fileStat)
  return statObj;
}

JNIEXPORT jstring JNICALL Java_pac_util_FileStat_getMimetype0
  (JNIEnv *env, jclass c, jstring filename) {
  
  // acquire a magic cookie...
  magic_t magic_cookie = magic_open(MAGIC_MIME);
  if (magic_cookie == NULL) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), 
      "unable to initialize magic library\n");
  }
    
  // load the default magic file...
  if (magic_load(magic_cookie, NULL) != 0) {
    const char* errorChars = magic_error(magic_cookie);
    char* exBuffer = malloc(sizeof(char)*(strlen(errorChars)+33));
    sprintf(exBuffer, "unable to load magic database - %s", errorChars);
    magic_close(magic_cookie);
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), exBuffer);
    free(exBuffer);
    return NULL;
  }
    
  // determine the mimetype from the filename
  const jbyte* filenameChars = (*env)->GetStringUTFChars(env, filename, NULL);
  const char* magic_full = magic_file(magic_cookie, filenameChars);
    
  // pick out the mimetype...
  int pos = strcspn(magic_full, ";");
  char* result = malloc(sizeof(char) * (pos + 1));
  memcpy(result, magic_full, pos);
  result[pos] = '\0';
 
  // create a java String from the mimetype
  jstring mimetypeStr = (*env)->NewStringUTF(env, result);
    
  // cleanup...
  (*env)->ReleaseStringUTFChars(env, filename, filenameChars);
  free(result);
  magic_close(magic_cookie);
    
  return mimetypeStr;
}

JNIEXPORT jstring JNICALL Java_pac_util_FileStat_realpath0
  (JNIEnv *env, jclass c, jstring filename) {

  // convert filename String to char*
  const jbyte* filenameChars = (*env)->GetStringUTFChars(env, filename, NULL);

  // get the realpath
  char* realpathChars = realpath(filenameChars, NULL);
  if (realpathChars == NULL) {
    char* exBuffer = malloc(sizeof(char)*(strlen(filenameChars)+35));
    sprintf(exBuffer, "error resolving canonical path of %s", filenameChars);
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), exBuffer);
    free(exBuffer);
    return NULL;
  }

  // convert char* to String and perform cleanup
  jstring realpathStr = (*env)->NewStringUTF(env, realpathChars);
  (*env)->ReleaseStringUTFChars(env, filename, filenameChars);
  free(realpathChars);

  return realpathStr;
}
