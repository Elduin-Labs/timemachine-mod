# Time Machine

A block that turns the world back to any version of Minecraft, from the four-day prototype Notch
put on TIGSource in May 2009 through to the present.

It obviously cannot swap out the jar you are running. What it does instead is put the rules and
the contents of the game back the way they were on the date you dial in. Set it to Beta 1.7.3 and
hunger stops draining, sprinting stops working, nothing drops experience, every swing lands for
full damage again, and anything invented after July 2011 is taken off you and held until you come
forward. Set it to Classic and you are left with the blocks that existed in 2009.

Built for Minecraft 1.21.11 on Fabric.

## Using it

Craft the machine, place it, right-click it:

```
O E O      O  obsidian        E  eye of ender
R C R      R  block of redstone
O O O      C  clock
```

The dial lists every version. Pick one and the right-hand panel tells you what the trip will cost
— worked out against your actual inventory, so it says "your netherite pickaxe", not "some items".
Then pull the lever. Arrow keys turn the dial, Enter engages.

`/timemachine now` says where you are and what has not been invented yet. `/timemachine list`
prints the dial. `/timemachine vault` says how much of your gear the future is holding.
`/timemachine set <version>` is the operator escape hatch.

## What actually changes

**Your inventory.** Anything from later than the current version is taken and held in a vault
until you travel to a version that has it, then handed straight back — enchantments, damage,
custom names and all. Nothing is destroyed. This covers your inventory, armour, off-hand, the item
on your cursor, and your ender chest.

**What walks around.** Mobs from later versions stop existing, with a puff of portal particles.
This is enforced on a one-second sweep rather than at every spawn site, so a warden that wanders in
from a spawn egg, a spawner or a breeding pen all go the same way.

**What you can craft.** Recipes whose *result* postdates the era stop matching. Most future
recipes are already impossible because their ingredients were confiscated, but the ones that
matter are the recipes with ancient ingredients and a modern result: shields, pistons, beds,
hoppers, anvils.

**The rules of the game**, via mixins, per feature:

| Feature | Arrived | Off before then |
| --- | --- | --- |
| Hunger | Beta 1.8 | Bar frozen full; food heals you directly instead, at one health per nutrition point |
| Sprinting | Beta 1.8 | The flag refuses to turn on |
| Experience | Beta 1.8 | No orb ever spawns |
| Attack cooldown | 1.9 | Cooldown progress pinned to 1 — every swing lands full |
| Off-hand | 1.9 | Anything in the slot is pushed back into the inventory |
| Elytra | 1.9 | Nothing glides |
| Swimming | 1.13 | The swim pose and its speed never engage |
| The Nether | Alpha 1.2 | You are put back in the overworld |
| The End | 1.0.0 | Likewise |

**What does not change: the world.** Blocks you already placed stay exactly where they are. A time
machine that deleted your base every time you turned the dial would be a griefing tool, not a toy.

## How things are dated

`ContentCalendar` dates every item and mob. A thousand items cannot be dated one line at a time
and most do not need to be, because Minecraft names things after the update that added them —
everything with `deepslate` in its id is Caves & Cliffs, everything with `crimson` is the Nether
Update. So the calendar is a list of rules, newest first, first match wins. That ordering is
load-bearing: an explicit newer entry always beats an older substring rule, and only the reverse
case (an ancient item swept up by a modern rule, like `soul_sand` under `soul_`) needs a
hand-written exception.

Anything unrecognised is dated to Indev, when crafting and most of the basic palette appeared.
That is deliberately the conservative answer — a gap in the table can never wrongly confiscate
something in the eras people actually visit. Items outside the `minecraft` namespace, including
the Time Machine itself, are exempt entirely; confiscating the machine would strand you in the
Stone Age with no way home.

The dating is good, not perfect. A handful of blocks are placed a version or two out in the deep
past where the historical record is fuzzy and nobody is checking.

## Verifying it

A clean `./gradlew build` proves nothing here. The real test boots a client:

```bash
./gradlew runClientGameTest
```

It builds a world, places a machine, right-clicks it, winds the dial back with the keyboard and
pulls the lever — then checks the past actually arrived: netherite confiscated and returned,
a warden gone and a pig left standing, sprinting refused, the cooldown pinned, hunger frozen while
the same effect drains it in the present, no experience orb, a shield that will not craft, and the
Nether closed in Classic but open in Beta. Every check has a control that runs in the present, so
a check cannot pass because its setup silently failed.

The harness exits 0 when the client dies partway through a run, so the last thing the test does is
write `run/timemachine-gametest-passed.txt`. That file existing is the only honest proof.

Textures come from `tools/gen_assets.py`; everything it writes is checked in, so a build never
depends on running it.
