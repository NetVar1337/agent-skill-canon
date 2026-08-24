> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/thread.md).

# Thread

Registered with `register_addon_thread(engine)`.

Three types — `mutex`, `lock_guard` and `cond_var` — plus three free functions. `mutex` is a reader-writer lock, so one handle serves both exclusive and shared locking.

## mutex

```c
void mutex()                  // default-constructed, unlocked

void m.lock()                 // exclusive (writer); blocks
void m.unlock()
bool m.try_lock()             // false if it would block

void m.lock_shared()          // shared (reader); concurrent holders allowed
void m.unlock_shared()
bool m.try_lock_shared()
```

Use `lock` / `unlock` for plain mutual exclusion, and the shared variants when several readers can run together but a writer needs to be alone.

```c
mutex m;

m.lock();
// exclusive section
m.unlock();

bool got = m.try_lock();       // true
if (got) { m.unlock(); }

m.lock_shared();
// read-only section; other readers may be in here too
m.unlock_shared();
```

## lock\_guard

RAII over a mutex: the constructor locks, the destructor unlocks. Copies are rejected at run time, since two guards cannot both own one lock.

```c
void lock_guard(mutex m)      // locks m; unlocks it at scope exit
```

```c
mutex m;
{
    lock_guard g = lock_guard(m);
    // m is held here
}                               // scope exit -> dtor -> m released

m.try_lock();                   // true — the guard released it
```

```c
lock_guard a = lock_guard(m);
lock_guard b = a;               // run-time error: "lock_guard is non-copyable"
```

## cond\_var

Waits on a `mutex` you already hold. `wait` releases it, blocks, and reacquires it before returning.

```c
void cond_var()               // default-constructed

void cv.wait(mutex m)         // caller must hold m exclusively
void cv.notify_one()
void cv.notify_all()
```

A wait can wake spuriously, so re-check the condition in a loop rather than assuming a wakeup means the state changed.

```c
mutex m;
cond_var cv;

// Producer
m.lock();
queue_push(x);
cv.notify_one();
m.unlock();

// Consumer
m.lock();
while (!ready()) {
    cv.wait(m);       // releases m, waits, reacquires m
}
m.unlock();
```

## Free functions

```c
void  sleep_us(int64 microseconds)
void  yield_cpu()               // give up the rest of this scheduling quantum
int64 hardware_threads()        // cores the platform reports
```

```c
sleep_us(1000);                 // 1 ms
yield_cpu();
println(std::to_string(hardware_threads()));
```

## Across threads

The addon provides the primitives, not the threads — the host spawns those from native code and shares handles with the script. `mutex` and `cond_var` handles are safe to pass across threads.
