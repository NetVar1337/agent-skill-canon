> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/inheritance.md).

# Inheritance & Access

## Access control

`struct` members are public by default; `class` members are private. Sections change the default for everything after them.

```c
class Entity {
    int32 secret;              // private — the class default
public:
    int32 hp;
    Entity(int32 h) { hp = h; }
    virtual int32 get_hp() { return hp; }
protected:
    void heal(int32 n) { hp = hp + n; }   // derived classes only
}
```

C++ rules are enforced: a derived class cannot reach a base's privates.

`friend class B;` grants `B` access to this class's members, and `friend Ret f(Args);` grants it to that one free-function **signature** — not to every overload of the name. See [Structs & Classes](/perception/enma-lang/language-guide/structs-and-classes.md#friend).

### protected is granted through your own type

A protected inherited member is reachable from a method of `C` only through an object whose static type is `C` or derived from `C`. Reaching it through a plain `Base&` is rejected, even from inside a derived class.

## Base clauses carry their own access

`class D : public B`, `: private B`, `: protected B` — in any order with `virtual` (`: public virtual A`).

**The default follows the class key**: `struct` bases are public, `class` bases are private. So a bare `class D : B` inherits *privately*, and the derived-to-base conversion is then available only inside `D` and its friends — an upcast from outside is a compile error.

```c
class Entity {
public:
    virtual int32 get_hp() { return 0; }
}

class Player : public Entity {     // `public` is required for the upcast below
public:
    int32 hp;
    int32 armor;
    Player(int32 h, int32 a) { hp = h; armor = a; }
    int32 get_hp() override { return hp + armor; }
}

Player* p = new Player(100, 50);
Entity* e = p;              // fine — the base is public
int32 hp = e->get_hp();     // 150, dispatched to Player::get_hp
delete p;
```

Write `class Player : Entity` instead and `Entity* e = p;` is rejected: the base is private.

`struct` needs no keyword for the same effect — its bases are already public.

## Virtual dispatch

A method call through a base-type pointer or reference reaches the derived implementation via the vtable. `override` goes after the parameter list.

Overriding is decided by the whole signature, not the name: parameter types (aliases resolved), the trailing `const`, and a return type that is identical or covariant.

**Slicing copies the object, not its type.** `Base b = derived;` copies Base's subobject and leaves `b` a `Base` — virtual calls on it reach Base's overrides. The vptr belongs to the object being built, not to the bytes being copied.

## Abstract classes

`virtual T f() = 0;` declares a pure virtual; an `interface` method with no body is one too. A class with a pure virtual that nothing implements — its own or inherited and not overridden — is **abstract**: it cannot be the type of a variable, a by-value parameter, a return type, or a data member, and it cannot be `new`ed. Pointers and references to it are the point of declaring one, and it may still declare constructors and a destructor for derived classes to use.

## final and name hiding

`final` on a class forbids deriving from it, and on a virtual method forbids overriding it further.

**Name hiding**: a derived method with the same name but a different signature *hides* the inherited one rather than overloading it. The base version stays reachable through a base pointer or by qualifying, and `override` on such a method is an error - it overrides nothing.

## dynamic\_cast

`dynamic_cast<T*>(p)` performs down- and cross-casts through the hierarchy, yielding `null` when the object is not a `T`.

A `static_cast` downcast **across a virtual base is rejected** - use `dynamic_cast` there.

## Multiple inheritance

Each base is laid out as its own subobject in declaration order, and the derived class inherits fields and methods from every base.

```c
struct Health {
    int32 hp;
    Health() { hp = 100; }
    void take_damage(int32 d) { hp = hp - d; }
}

struct Mana {
    int32 mp;
    Mana() { mp = 50; }
    void spend(int32 cost) { mp = mp - cost; }
}

struct Player : Health, Mana {
    int32 xp;
    Player() { xp = 0; }
}

Player* p = new Player();
p->take_damage(30);  // Health.hp = 70
p->spend(15);        // Mana.mp = 35 — `this` auto-adjusted to the Mana subobject
delete p;
```

**Construction order.** Bases construct in declaration order before the derived body runs, and destruct in reverse.

**Bases past the first need a `this`-adjusted pointer.** Calling a method from a non-primary base compiles in a pointer adjustment automatically, and virtual dispatch through a non-primary base pointer finds derived overrides via a `this`-adjusting thunk.

**Explicit base init lists.** When a base needs constructor arguments:

```c
struct Player : Health, Mana {
    int32 xp;
    Player(int32 h, int32 m) : Health(h), Mana(m) { xp = 0; }
}
```

Bases without an entry use their no-arg constructor.

**Name conflicts across bases.** Two bases may declare the same name; what an *unqualified* reference to it does is become ambiguous. Name the path (`p->B::m()`, `d.B::v`) or convert to the base subobject you mean.

**Upcast** to a base pointer is implicit in all four conversion contexts — variable declaration, function argument, assignment, and return — shifting the pointer to the base subobject as needed.

## Diamonds

When `B` and `C` both derive from `A` and `D` derives from both, whether `A` is duplicated or shared is decided by `virtual`. **The two forms are different types with different layouts, not two spellings of one thing.**

### Non-virtual — two independent subobjects

```c
struct A { int32 v; }
struct B : A { }
struct C : A { }
struct D : B, C { }     // D holds TWO independent A subobjects
```

Each has its own storage and its own lifetime; `A`'s constructor and destructor each run twice, in declaration order and reverse. An unqualified `d.v` is **ambiguous** — name the path (`d.B::v`) or convert to the base subobject you mean.

### Virtual — one shared subobject

```c
struct A { int32 v; }
struct B : virtual A { }
struct C : virtual A { }
struct D : B, C { }     // one shared A
```

Making both paths `virtual A` collapses them to a single subobject, constructed and destroyed exactly once and initialized by the most-derived class. `d.v` is then unambiguous.

## Interfaces

Method-signature contracts that structs implement — all-virtual, no state.

```c
interface Shape {
    int32 area();
    int32 kind();
}

struct Rect : Shape {
    int32 w;
    int32 h;
    Rect(int32 w_, int32 h_) { w = w_; h = h_; }
    int32 area() override { return w * h; }
    int32 kind() override { return 1; }
}
```

For SDK-registered interfaces (`.as_interface()` + `.implements(...)`), an interface-typed local dispatches at runtime, including across reassignment:

```c
Stream s = file_stream(path);   // concrete impl assigned to an iface var
int64 n = s.write("hello");     // dispatches to file_stream.write
s = mem_stream(...);
int64 m = s.write("...");       // dispatches to mem_stream.write
```
