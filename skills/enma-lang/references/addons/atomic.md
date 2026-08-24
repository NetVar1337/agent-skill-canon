> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/atomic.md).

# Atomic

Registered with `register_addon_atomic(engine)`. Auto-imported.

The primitive `aint8` / `aint16` / `aint32` / `aint64` types are already atomic at the language level — plain reads and writes, and the compound forms `+= |= &= ^= ++ --`, all compile to interlocked instructions.

```c
aint64 counter = 0;
counter = counter + 1;      // atomic on its own
```

The wrapper types here add the explicit method spellings — the ones that hand back the previous value, and compare-and-swap. Every operation uses sequentially consistent ordering; relaxed, acquire and release variants are not exposed.

## atomic\_int32 / atomic\_int64

Both are value structs wrapping a single atomic field — no heap allocation and no destructor. They carry the same methods; only the stored width differs.

```c
atomic_int32(int64 initial)      // the value is stored as int32
atomic_int64(int64 initial)

int64 a.load()
void  a.store(int64 value)
int64 a.exchange(int64 value)                            // returns the PREVIOUS value
bool  a.compare_exchange(int64 expected, int64 desired)  // true if the swap happened

int64 a.add(int64 value)         // returns the PREVIOUS value
int64 a.sub(int64 value)         // returns the PREVIOUS value
int64 a.bit_and(int64 mask)      // returns the PREVIOUS value
int64 a.bit_or(int64 mask)       // returns the PREVIOUS value
int64 a.bit_xor(int64 mask)      // returns the PREVIOUS value

int64 a.inc()                    // returns the NEW value
int64 a.dec()                    // returns the NEW value
```

`inc` and `dec` are the two that report the new value; everything else reports what was there before.

```c
atomic_int64 a = atomic_int64(10);

a.load();            // 10
a.store(5);
a.exchange(9);       // 5  — and a is now 9
a.add(3);            // 9  — and a is now 12
a.sub(2);            // 12 — and a is now 10
a.inc();             // 11
a.dec();             // 10
a.bit_or(0xF0);      // 10 — and a is now 250
a.bit_and(0x0F);     // 250 — and a is now 10
```

`compare_exchange` writes `desired` only if the current value still equals `expected`, which is what makes a read-modify-write safe against a racing writer.

```c
atomic_int64 x = atomic_int64(0);

int64 double_it() {
    int64 retries = 0;
    while (true) {
        int64 current = x.load();
        if (x.compare_exchange(current, current * 2)) { return retries; }
        retries = retries + 1;      // someone else won; read again and retry
    }
}
```

## spinlock

Busy-waits instead of sleeping, so it suits very short critical sections where a `mutex` handoff would cost more than the spin.

```c
spinlock()             // default-constructed, unlocked

void sl.lock()         // spins until acquired
bool sl.try_lock()     // false if already held
void sl.unlock()
```

```c
spinlock sl;

sl.lock();
// critical section
sl.unlock();

if (sl.try_lock()) { sl.unlock(); }
```

For anything longer, use `mutex` from the [thread addon](/perception/enma-lang/addons/thread.md), which blocks rather than burning the core.

## Barriers

```c
void memory_barrier()
void read_barrier()
void write_barrier()
```

All three emit the same full fence. The three spellings exist to state intent at the call site, not to select three different orderings.
