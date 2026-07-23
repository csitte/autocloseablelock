# AutoCloseable Lock

Lightweight utility for safely handling Java locks with `try-with-resources`.  
Provides wrapper classes for `ReentrantLock` and `ReentrantReadWriteLock` that implement `AutoCloseable`.

[![Maven Central](https://img.shields.io/maven-central/v/com.csitte/autocloseablelock.svg)](https://search.maven.org/artifact/com.csitte/autocloseablelock)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/java-8+-brightgreen.svg)](https://adoptium.net/)

Available on [Maven Central Repository](https://mvnrepository.com/artifact/com.csitte/autocloseablelock)

## Quick Start

Add the following dependency to your `<dependencies>` section in `pom.xml`:

```xml
<dependency>
  <groupId>com.csitte</groupId>
  <artifactId>autocloseablelock</artifactId>
  <version>1.3</version>
</dependency>
```

## Description

The `autocloseablelock` package provides a convenient and reliable way to use Java locks.
It leverages `try-with-resources` to ensure that locks are always released safely, even when exceptions occur.

Additional features include:

- Transparent handling of `InterruptedException`
- Timeout support with compensation for [spurious wakeups](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/Condition.html)
- Read-write lock support (`CloseableReadWriteLock`)
- Integrated condition handling (`LockCondition`)

Internally, it wraps the [`java.util.concurrent.locks`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/package-summary.html) API and enhances it with usability and safety improvements.

## Basic Usage

Instead of the traditional way:

```java
Lock myLock = new ReentrantLock();
void method()
{
    myLock.lock();
    try
    {
        doSomething();
    }
    finally
    {
        myLock.unlock();
    }
}
```
        
Use it with the try-with-resources feature:

```java
CloseableLock myLock = new CloseableLock(new ReentrantLock());
void method()
{
    try (AutoCloseableLock ignored = myLock.lock())
    {
        doSomething();
    }
}
```
        
The `CloseableLock` class provides a wrapper for a `java.util.concurrent.locks.Lock` object.
By default, a `ReentrantLock` is used, but any other `Lock`-implementation can be provided in the constructor.
When the scope is exited, it is ensured that the lock will be released.

## Try lock with timeout

```java
CloseableLock myLock = new CloseableLock();
Duration timeout = Duration.ofSeconds(10);
try (AutoCloseableLock ignored = myLock.tryLock(timeout))
{
    doSomething();
}
catch (LockTimeoutException runtimeException)
{
    // do appropriate error handling
}
```

If the lock cannot be acquired before the timeout duration expires, then a `LockTimeoutException` is thrown.

## Wait

The `wait()` method does what its name says: it waits for the specified time.

```java
new CloseableLock().wait(Duration.ofSeconds(10));
```
        
## Wait for condition

You can use a `BooleanSupplier` as an argument to test if a condition is true until the given timeout expires.

```java
CloseableLock myLock = new CloseableLock();
if (myLock.waitForCondition(()->isReady(), Duration.ofSeconds(10))
{
    # condition which is tested by isReady() is true. Continue processing...
}
else
{
    # condition is not ready after 10 seconds. Do error handling...
}
```
            
A timeout value of zero means that the method only returns when the condition is true.
Any other thread can use the `CloseableLock.signalAll()` method to signal waiting threads of a change in condition.
Waiting threads are woken by `signal()`/`signalAll()` and re-check the condition under the lock.

## ReadWriteLock

Use `CloseableReadWriteLock` if you need the [`ReadWriteLock`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/locks/ReadWriteLock.html) 
functionality and also want the benefits of `AutoCloseable`.
The read-lock may be held simultaneously by multiple threads as long as there is no write-lock.
A unique feature is the ability to downgrade a write-lock to a read-lock without losing the hold on the lock.

```java
CloseableReadWriteLock readWriteLock = new CloseableReadWriteLock(new ReentrantReadWriteLock());
void method()
{
    try (AutoCloseableWriteLock acwl = readWriteLock.writeLock())
    {
        modifyProtectedResource();
        
        acwl.downgradeToReadLock();
        
        doReadOnlyActivity();
    }
}
```
        
The `CloseableReadWriteLock` has the following locking-methods:

```java
AutoCloseableWriteLock writeLock()
AutoCloseableWriteLock writeLockInterruptibly()
AutoCloseableWriteLock tryWriteLock(Duration)
AutoCloseableLock readLock()
AutoCloseableLock tryReadLock(Duration)
```

The methods that acquire a write-lock return an `AutoCloseableWriteLock` object,
which should be used in a try-with-resources block to ensure that the lock is released afterwards.
Inside the block, the following methods can be used with `AutoCloseableWriteLock`:

```java
void wait(Duration timeout)
boolean waitForCondition(BooleanSupplier fCondition, Duration timeout)
void signalAll()
void signal()
void downgradeToReadLock()
void downgradeToReadLockInterruptibly()
```

## LockCondition

This class represents a state. It is bound to a lock. If the state of the `LockCondition` changes, 
this is signaled to all waiting threads. The `setState()` method acquires the lock before changing the state.

```java
// Example
enum STATE { INIT, ACTIVE, FINISHED }
CloseableLock myLock = new CloseableLock();
LockCondition<STATE> state = new LockCondition<>(myLock, STATE.INIT);

void doActivity()
{
    state.setState(STATE.ACTIVE);
    doSomething();
    state.setState(STATE.FINISHED);
}

void waitUntilActivityHasFinished()
{
    myLock.waitForCondition(() -> state.getState()==STATE.FINISHED, timeout);    
}

// Another example using a 'String' as state-variable.
CloseableLock myLock = new CloseableLock();
LockCondition<String> condition = new LockCondition<>(myLock, "Init");

void doActivity()
{
    condition.setState("Active");
    doSomething();
    condition.setState("Finished");
}

void waitUntilActivityHasFinished()
{
    myLock.waitForCondition(() -> condition.getState().equals("Finished"), timeout);    
}
```

## BooleanLockCondition

This is a convenience class for `LockCondition<Boolean>`. It has an initial default value of `FALSE`.

```java
CloseableLock myLock = new CloseableLock();
BooleanLockCondition finished = new BooleanLockCondition(myLock);

void doActivity()
{
    doSomething();
    finished.setState(true);
}

void waitUntilActivityHasFinished()
{
    myLock.waitForCondition(() -> finished.isTrue(), timeout);    
}
```

## Building from Source

### Requirements
- Java 8+ JDK (the project targets Java 8 for maximum compatibility)
- Apache Maven 3.6.3 or newer

### Quick Build
```bash
git clone https://github.com/csitte/autocloseablelock.git
cd autocloseablelock
mvn clean verify
```

This runs:
- Compilation
- All unit tests
- PMD static analysis and copy-paste detection (CPD)
- Attaches source and javadoc JARs

A plain `mvn clean install` or `mvn clean verify` works for everyday development without any special flags.

### Maven Profiles

| Profile   | Purpose                                      | Typical Command                     | Notes |
|-----------|----------------------------------------------|-------------------------------------|-------|
| (none)    | Standard development / CI build              | `mvn clean verify`                  | No GPG signing. Recommended for local work. |
| `release` | Enable GPG signing of artifacts              | `mvn clean verify -P release`       | Required for Maven Central publication. Needs configured GPG key. |
| `snapshot`| Configure snapshot distribution repository   | `mvn ... -P snapshot`               | Rarely needed directly. |

To perform a full release build with signatures:

```bash
mvn clean verify -P release
```

**GPG note**: The `release` profile activates `maven-gpg-plugin`. You must have a GPG secret key available (and usually Sonatype Central credentials configured in `~/.m2/settings.xml` or via environment). See the [Sonatype GPG requirements](https://central.sonatype.org/publish/requirements/gpg/).

## Releasing (Maintainers)

This project is published to [Maven Central](https://search.maven.org/artifact/com.csitte/autocloseablelock) using the [central-publishing-maven-plugin](https://central.sonatype.org/publish/publish-portal-maven/).

High-level release flow:
1. Update version in `pom.xml` (remove `-SNAPSHOT` for the release).
2. Commit and tag the release.
3. Run a signed build: `mvn clean deploy -P release` (or follow the Central Portal deployment process).
4. After successful publication, bump the version to the next `-SNAPSHOT`.

The GitHub repository contains the authoritative history and any additional release notes.
