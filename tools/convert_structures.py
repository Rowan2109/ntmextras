# SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
# SPDX-License-Identifier: LGPL-3.0-only
"""Converts HBM's 1.7.10 NBT structures (palette of block names + metadata, wand blocks, tile entities)
to vanilla structure templates for 26.2 with NTM NEXT blocks.

    HBM_1710_SRC=... NTM_NEXT_SRC=... py -3 -I convert_structures.py

Reads the upstream .nbt files, writes data/ntmextras/structure/*.nbt, the loot tables the wand blocks ask for and
build/structure-conversion.json (per structure: what went in, what came out, what was dropped). A block without a mapping stops the
run and is listed; nothing is silently replaced with air.
"""
import collections
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbtio
from nbtio import Byte, Float, Int, String, TagList, ints

UPSTREAM = os.environ['HBM_1710_SRC'] + '/src/main/resources/assets/hbm/structures'     # checkout of HbmMods/Hbm-s-Nuclear-Tech-GIT
HBM_TABLES = os.environ['NTM_NEXT_SRC'] + '/neoforge/src/main/resources/data/hbm/loot_table/chests'     # checkout of ntm-next
HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(os.path.dirname(HERE), 'src', 'main', 'resources', 'data', 'ntmextras')
OUT_STRUCTURES = os.path.join(DATA, 'structure')
OUT_TABLES = os.path.join(DATA, 'loot_table', 'chests')
REPORT = os.path.join(os.path.dirname(HERE), 'build', 'structure-conversion.json')
DATA_VERSION = 4903             # what NTM NEXT's own templates carry

STRUCTURES = ('falloutshelter_house', 'abandoned_arcticstation', 'military_base', 'submarine')

DIR = {2: 'north', 3: 'south', 4: 'west', 5: 'east'}
DIR6 = {0: 'down', 1: 'up', 2: 'north', 3: 'south', 4: 'west', 5: 'east'}
STAIRS = {0: 'east', 1: 'west', 2: 'south', 3: 'north'}
DOOR = {0: 'east', 1: 'south', 2: 'west', 3: 'north'}
TRAPDOOR = {0: 'north', 1: 'south', 2: 'west', 3: 'east'}
BED = {0: 'south', 1: 'west', 2: 'north', 3: 'east'}
AXIS = {0: 'y', 4: 'x', 8: 'z'}
# The wand block's rotation is where the placer stood; the container faces the placer.
LOOT_FACING = {0: 'south', 1: 'west', 2: 'east', 3: 'north'}
BUTTON = {1: 'east', 2: 'west', 3: 'south', 4: 'north'}
COLOURS = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
           'silver', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')
VANILLA_COLOURS = tuple('light_gray' if c == 'silver' else c for c in COLOURS)
PLANKS = ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak')
CONCRETE_EXT = ('machine', 'machine_stripe', 'indigo', 'purple', 'pink', 'hazard', 'sand', 'bronze')
PLATEMETAL = ('base', 'black', 'white', 'red', 'green', 'light_gray', 'gray', 'blue', 'cyan', 'light_blue',
              'brown', 'orange', 'purple', 'pink', 'yellow', 'lime')
BRICK_SLAB = ('reinforced_stone', 'reinforced_brick', 'brick_obsidian', 'brick_light', 'brick_compound',
              'brick_asbestos', 'brick_fire')
CONCRETE_SLAB = ('concrete_smooth', 'concrete', 'concrete_asbestos', 'ducrete_smooth', 'ducrete', 'asphalt')
STALACTITE = ('sulfur', 'asbestos', 'ice', 'snow')
SCAFFOLD = ('steel_scaffold', 'steel_scaffold_red', 'steel_scaffold_white', 'steel_scaffold_yellow')
SCAFFOLD_ORIENT = {0: 'ns_upright', 4: 'ew_flat', 8: 'ew_upright', 12: 'ns_flat'}
CRASHED = ('crashed_balefire', 'crashed_conventional', 'crashed_nuke', 'crashed_salted')
CRT = ('deco_crt_clean', 'deco_crt_broken', 'deco_crt_blinking', 'deco_crt_bsod')
BOBBLES = ('none', 'strength', 'perception', 'endurance', 'charisma', 'intelligence', 'agility', 'luck', 'bob',
           'frizzle', 'pu238', 'vt', 'doc', 'bluehat', 'pheo', 'adam29', 'uffr', 'vaer', 'nos', 'drillgon', 'cirno',
           'microwave', 'peep', 'mellow', 'abel')
# Blocks that came out of hbm's BlockDummyable: only the core survives, NTM NEXT builds the rest itself.
MULTIBLOCK_CORES = {'vending_machine', 'launch_pad_rusted', 'cargo_door', 'silo_hatch'}
# Same name, no state, no meta. Plain cubes unless listed in NOT_SOLID below.
PLAIN = {
    'asphalt', 'block_fallout', 'block_lead', 'brick_asbestos', 'brick_concrete', 'brick_concrete_broken',
    'brick_concrete_cracked', 'brick_light', 'concrete', 'concrete_asbestos', 'concrete_smooth', 'crate', 'crate_ammo',
    'crate_can', 'crate_lead', 'crate_weapon', 'deco_aluminium', 'deco_beryllium', 'deco_lead', 'deco_red_copper',
    'deco_rusty_steel', 'deco_steel', 'deco_titanium', 'deco_tungsten', 'glass_ash', 'pole_top', 'reinforced_brick',
    'reinforced_light', 'reinforced_stone', 'tile_lab', 'tile_lab_broken', 'tile_lab_cracked', 'steel_roof',
    'reinforced_glass_pane', 'barrel_corroded', 'sandbags',
}
FACING_METAS = {'steel_wall', 'steel_corner', 'steel_poles', 'steel_beam', 'ladder_steel', 'ladder_sturdy',
                'tape_recorder', 'pole_satellite_receiver', 'machine_diesel', 'radiorec', 'wood_barrier'}
PIPES = {'deco_pipe', 'deco_pipe_rusted', 'deco_pipe_rim', 'deco_pipe_rim_rusted', 'deco_pipe_framed',
         'deco_pipe_framed_rusted', 'deco_pipe_quad', 'deco_pipe_quad_rusted'}
HBM_DOORS = {'door_metal', 'door_bunker'}
LOGIC_DISGUISE = {'deco_rusty_steel', 'deco_steel', 'deco_tungsten', 'crate', 'crate_iron', 'crate_weapon',
                  'machine_controller'}
WOOD_STAIRS = {'oak_stairs', 'spruce_stairs', 'birch_stairs', 'brick_stairs'}
POOLS = {'POOL_FILING_CABINET': 'filing_cabinet', 'POOL_OFFICE_TRASH': 'office_trash', 'POOL_SUPPLIES': 'supplies',
         'POOL_MACHINE_PARTS': 'machine_parts', 'POOL_BLUEPRINTS': 'blueprints', 'POOL_WEAPONS': 'weapons',
         'POOL_VAULT_RUSTY': 'vault_rusty', 'POOL_NUKE_FUEL': 'nuke_fuel', 'POOL_SILO': 'silo'}
LOOT_PILES = {'LOOT_CAPNUKE', 'LOOT_MEDICINE', 'LOOT_MAKESHIFT_GUN', 'LOOT_FLAREGUN', 'LOOT_SHIT', 'LOOT_BONES',
              'LOOT_MECHANICAL'}


class Out:
    """One converted position: block name, properties, block entity data."""
    __slots__ = ('name', 'props', 'nbt')

    def __init__(self, name, props=None, nbt=None):
        self.name = name
        self.props = dict(props or {})
        self.nbt = nbt


class Unmapped(Exception):
    pass


def plain_meta(meta, *expected):
    if meta not in expected:
        raise Unmapped('meta %d' % meta)


def stairs(name, meta):
    return Out(name, {'facing': STAIRS[meta & 3], 'half': 'top' if meta & 4 else 'bottom', 'shape': 'straight'})


def slab(name, meta):
    return Out(name, {'type': 'top' if meta & 8 else 'bottom'})


def door(name, meta):
    # Upper half holds the hinge, lower half holds the facing and open state. Joined up in link_doors().
    if meta & 8:
        return Out(name, {'half': 'upper', 'hinge': 'right' if meta & 1 else 'left', 'powered': 'false'})
    return Out(name, {'half': 'lower', 'facing': DOOR[meta & 3], 'open': 'true' if meta & 4 else 'false',
                      'powered': 'false'})


def chest_table(pool, low, high, used):
    """The wand block rolls min + nextInt(max - min) stacks, so the top of the range is max - 1."""
    top = high - 1 if high > low else low
    if top <= 0:
        return None     # min 0, max 1 is an empty container upstream, and stays one
    if pool not in POOLS:
        raise Unmapped('loot pool ' + pool)
    base = POOLS[pool]
    name = '%s_%d_%d' % (base, low, top)
    if os.path.exists(os.path.join(HBM_TABLES, name + '.json')):
        return 'hbm:chests/' + name
    used[name] = (base, low, top)
    return 'ntmextras:chests/' + name


def wand_loot(meta, nbt, used):
    target = nbt['block'].value.replace('hbm:tile.', '')
    target_meta = nbt['meta'].value
    pool = nbt['pool'].value
    low, high = nbt['min'].value, nbt['max'].value
    if nbt.get('lockCode', Int(0)).value != 0:
        raise Unmapped('locked loot container')
    if target == 'deco_loot':
        if pool not in LOOT_PILES:
            raise Unmapped('loot pile ' + pool)
        return Out('hbm:deco_loot', None, {'id': String('hbm:loot'), 'pool': String(pool)})
    table = chest_table(pool, low, high, used)
    if target in ('crate_iron', 'crate_steel'):
        out = Out('hbm:' + target)
        be = 'hbm:' + target
    elif target == 'filing_cabinet':
        name = ('filing_cabinet_green', 'filing_cabinet_steel')[target_meta]
        out = Out('hbm:' + name, {'facing': LOOT_FACING[meta]})
        be = 'hbm:file_cabinet'
    else:
        raise Unmapped('loot container ' + target)
    out.nbt = {'id': String(be)}
    if table:
        out.nbt['LootTable'] = String(table)
    return out


def wand_logic(meta, nbt):
    # The wand block is exchanged for a logic block on its first tick upstream; here it is the logic block already.
    be = {'id': String('hbm:logic_block'), 'phase': Int(0), 'timer': Int(0), 'actionID': String(nbt['actionID'].value),
          'conditionID': String(nbt['conditionID'].value), 'direction': Int(nbt['rotation'].value)}
    if 'interactionID' in nbt:
        be['interactionID'] = String(nbt['interactionID'].value)
    if 'disguise' in nbt:
        disguise = nbt['disguise'].value.replace('hbm:tile.', '')
        if disguise not in LOGIC_DISGUISE:
            raise Unmapped('logic block disguise ' + disguise)
        return Out('hbm:logic_block', {'disguise': disguise}, be)
    return Out('hbm:logic_block_invis', None, be)


def hbm_block(short, meta, nbt, used):
    if short in PLAIN:
        return Out('hbm:' + short)
    if short in FACING_METAS:
        return Out('hbm:' + short, {'facing': DIR[meta]})
    if short in PIPES:
        return Out('hbm:' + short, {'axis': AXIS[meta]})
    if short in HBM_DOORS:
        return door('hbm:' + short, meta)
    if short.endswith('_stairs'):
        return stairs('hbm:' + short, meta)
    if short == 'wand_loot':
        return wand_loot(meta, nbt, used)
    if short == 'wand_logic':
        return wand_logic(meta, nbt)
    if short in MULTIBLOCK_CORES:
        if meta < 12:
            return None     # dummy cell
        be = None
        props = {'facing': DIR[meta - 10]}
        if short in ('cargo_door', 'silo_hatch'):
            state = nbt['state'].value
            props['open'] = 'true' if state == 1 else 'false'
            props['skin'] = '0'
            be = {'id': String('hbm:door'), 'state': Byte(state), 'openTicks': Int(nbt['openTicks'].value),
                  'redstoned': Int(nbt['redstoned'].value)}
        return Out('hbm:' + short, props, be)
    if short == 'brick_slab':
        return slab('hbm:%s_slab' % BRICK_SLAB[meta & 7], meta)
    if short == 'concrete_slab':
        return slab('hbm:%s_slab' % CONCRETE_SLAB[meta & 7], meta)
    if short == 'concrete_double_slab':
        return Out('hbm:%s_slab' % CONCRETE_SLAB[meta & 7], {'type': 'double'})
    if short == 'concrete_colored':
        return Out('hbm:concrete_' + COLOURS[meta])
    if short == 'concrete_colored_ext':
        return Out('hbm:concrete_ext_' + CONCRETE_EXT[meta])
    if short == 'concrete_pillar':
        return Out('hbm:concrete_pillar', {'axis': AXIS[meta]})
    if short == 'fence_metal':
        return Out(('hbm:fence_metal', 'hbm:fence_metal_post')[meta])
    if short == 'wood_structure':
        return Out('hbm:wood_structure_' + ('roof', 'scaffold', 'ceiling')[meta])
    if short == 'stalactite':
        return Out('hbm:stalactite_' + STALACTITE[meta])
    if short == 'platemetal':
        return Out('hbm:platemetal', {'variant': PLATEMETAL[meta]})
    if short == 'steel_grate':
        return Out('hbm:steel_grate', {'height': str(meta)})
    if short == 'steel_scaffold':
        return Out('hbm:' + SCAFFOLD[meta & 3], {'orient': SCAFFOLD_ORIENT[meta & 12]})
    if short == 'barbed_wire':
        return Out('hbm:barbed_wire', {'axis': 'x' if meta >= 4 else 'z'})
    if short == 'deco_computer':
        return Out('hbm:deco_computer', {'facing': {0: 'north', 4: 'south', 8: 'west', 12: 'east'}[meta]})
    if short == 'deco_crt':
        return Out('hbm:' + CRT[meta >> 2], {'rotation': str(meta & 3)})
    if short in ('spotlight_incandescent', 'spotlight_incandescent_off'):
        # Upstream's worldgen always turns these off and breaks the bulb. Beams stay what they were.
        return Out('hbm:spotlight_incandescent', {'broken': 'true', 'lit': 'false', 'facing': DIR6[meta >> 1]})
    if short == 'spotlight_beam':
        return Out('hbm:spotlight_beam', {'level': '15'})
    if short == 'machine_electric_furnace_off':
        return Out('hbm:machine_electric_furnace', {'facing': DIR[meta], 'lit': 'false'})
    if short == 'red_cable_gauge':
        return Out('hbm:red_cable_gauge', {'facing': DIR6[meta]})
    if short == 'cable_switch':
        return Out('hbm:cable_switch', {'open': 'false'})
    if short == 'trapdoor_steel':
        return Out('hbm:trapdoor_steel', {'facing': TRAPDOOR[meta & 3], 'half': 'top' if meta & 8 else 'bottom',
                                          'open': 'true' if meta & 4 else 'false', 'powered': 'false'})
    if short == 'bobblehead':
        # Upstream rolls a new type at placement; the processor list does that too.
        return Out('hbm:bobblehead', {'rotation': str(meta)},
                   {'id': String('hbm:bobblehead'), 'type': String(BOBBLES[nbt['type'].value])})
    if short == 'crashed_bomb':
        return Out('hbm:' + CRASHED[meta])
    if short == 'mine_ap':
        return Out('hbm:mine_ap', None, {'id': String('hbm:landmine'), 'primed': Byte(nbt['primed'].value),
                                         'waiting': Byte(nbt['waiting'].value)})
    if short in ('radio_torch_sender', 'radio_torch_receiver'):
        be = {'id': String('hbm:rtty_sender' if short.endswith('sender') else 'hbm:rtty_rec'), 'c': String(nbt['c'].value),
              'p': Byte(0), 'm': Byte(0), 'l': Int(0), 'u': nbtio.Tag(nbtio.LONG, 0)}
        for i in range(16):
            be['m%d' % i] = String(nbt['m%d' % i].value)
        return Out('hbm:' + short, {'facing': DIR6[meta], 'lit': 'false'}, be)
    raise Unmapped('no mapping')


def vanilla_block(short, meta, nbt):
    if short == 'air':
        return Out('minecraft:air')
    if short == 'dirt':
        plain_meta(meta, 0)
        return Out('minecraft:dirt')
    if short == 'stone':
        plain_meta(meta, 0)
        return Out('minecraft:stone')
    if short == 'grass':
        return Out('minecraft:grass_block', {'snowy': 'false'})
    if short == 'tallgrass':
        plain_meta(meta, 1)
        return Out('minecraft:short_grass')
    if short == 'snow_layer':
        return Out('minecraft:snow', {'layers': str((meta & 7) + 1)})
    if short == 'web':
        return Out('minecraft:cobweb')
    if short == 'glass_pane':
        return Out('minecraft:glass_pane')
    if short == 'fence':
        return Out('minecraft:oak_fence')
    if short == 'log':
        return Out('minecraft:oak_log', {'axis': AXIS[meta & 12]})
    if short == 'planks':
        return Out('minecraft:%s_planks' % PLANKS[meta])
    if short == 'wooden_slab':
        return slab('minecraft:%s_slab' % PLANKS[meta & 7], meta)
    if short == 'stone_slab':
        plain_meta(meta & 7, 4)
        return slab('minecraft:brick_slab', meta)
    if short in WOOD_STAIRS:
        return stairs('minecraft:' + short, meta)
    if short == 'bed':
        return Out('minecraft:red_bed', {'facing': BED[meta & 3], 'part': 'head' if meta & 8 else 'foot', 'occupied': 'false'})
    if short == 'carpet':
        return Out('minecraft:%s_carpet' % VANILLA_COLOURS[meta])
    if short == 'stained_glass':
        return Out('minecraft:%s_stained_glass' % VANILLA_COLOURS[meta])
    if short == 'stained_glass_pane':
        return Out('minecraft:%s_stained_glass_pane' % VANILLA_COLOURS[meta])
    if short == 'ladder':
        return Out('minecraft:ladder', {'facing': DIR[meta]})
    if short == 'wooden_door':
        return door('minecraft:oak_door', meta)
    if short == 'lever':
        # 5 and 6 stand on the floor, seen from the block's own rotation. Bit 8 is powered.
        low = meta & 7
        if low in (5, 6):
            return Out('minecraft:lever', {'face': 'floor', 'facing': 'north' if low == 5 else 'west',
                                           'powered': 'true' if meta & 8 else 'false'})
        raise Unmapped('lever meta %d' % meta)
    if short == 'stone_button':
        return Out('minecraft:stone_button', {'face': 'wall', 'facing': BUTTON[meta & 7],
                                              'powered': 'true' if meta & 8 else 'false'})
    if short == 'skull':
        plain_meta(meta, 1)
        if nbt['SkullType'].value != 0:
            raise Unmapped('skull type')
        return Out('minecraft:skeleton_skull', {'rotation': str(nbt['Rot'].value), 'powered': 'false'})
    if short == 'flower_pot':
        if nbt['Item'].value != 0:
            raise Unmapped('filled flower pot')
        return Out('minecraft:flower_pot')
    if short in ('water', 'flowing_water'):
        return Out('minecraft:water', {'level': str(meta)})
    raise Unmapped('no mapping')


# ----- neighbour dependent state ----------------------------------------------------------------------------------

NOT_SOLID = ('stairs', '_slab', 'fence', 'pane', 'door', 'ladder', 'barbed', 'carpet', 'snow', 'cobweb', 'short_grass',
             'flower_pot', 'skull', 'lever', 'button', 'bed', 'pipe', 'steel_wall', 'steel_corner', 'steel_poles',
             'steel_roof', 'steel_beam', 'scaffold', 'grate', 'bobblehead', 'computer', 'crt', 'sandbags', 'wood_barrier',
             'mine_', 'spotlight', 'stalactite', 'logic_block_invis', 'machine_', 'water', 'air', 'tape', 'pole_',
             'radio', 'red_cable_gauge', 'cable_switch', 'barrel', 'wood_structure', 'deco_loot', 'crashed', 'radiorec',
             'vending', 'launch_pad', 'cargo_door', 'silo_hatch', 'glass', 'crate_')


def solid_render(out):
    """Block.isSolidRender for what we place: opaque full cubes."""
    if out is None:
        return False
    n = out.name.split(':')[1]
    if n.endswith('_slab') and out.props.get('type') == 'double':
        return True
    if n.startswith('crate') and n not in ('crate_can', 'crate_lead', 'crate_ammo', 'crate_weapon'):
        return True
    return not any(k in n for k in NOT_SOLID) or n == 'platemetal'


def sturdy(out, towards):
    """isFaceSturdy for a neighbour, seen from the side that faces it. towards is the neighbour's side."""
    if out is None:
        return False
    if solid_render(out):
        return True
    n = out.name.split(':')[1]
    if 'glass' in n and 'pane' not in n:
        return True
    if n.startswith('crate') or n in ('cable_switch', 'red_cable_gauge'):
        return True
    if n.endswith('_stairs'):
        return out.props['facing'] == towards
    return False


def is_stairs(out):
    return out is not None and out.name.endswith('_stairs')


def is_pane(out):
    return out is not None and (out.name.endswith('_pane') or out.name == 'minecraft:iron_bars')


def is_fence(out):
    return out is not None and ('_fence' in out.name or out.name.startswith('hbm:fence_metal'))


HORIZONTAL = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}
OPPOSITE = {'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west'}
CCW = {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}
CW = {v: k for k, v in CCW.items()}


def add(pos, d):
    return pos[0] + d[0], pos[1] + d[1], pos[2] + d[2]


def link_doors(blocks):
    for pos, out in list(blocks.items()):
        if out is None or 'half' not in out.props or 'door' not in out.name or 'trapdoor' in out.name:
            continue
        if out.props['half'] != 'lower':
            continue
        top = blocks.get(add(pos, (0, 1, 0)))
        if top is None or top.name != out.name or top.props.get('half') != 'upper':
            raise Unmapped('door without its top half at %s' % (pos,))
        out.props['hinge'] = top.props['hinge']
        top.props['facing'] = out.props['facing']
        top.props['open'] = out.props['open']
    for pos, out in blocks.items():
        if out is not None and 'door' in out.name and 'trapdoor' not in out.name and 'facing' not in out.props:
            raise Unmapped('door top half without its bottom half at %s' % (pos,))


def stair_shape(blocks, pos, out):
    # StairBlock.getStairsShape
    facing = out.props['facing']
    half = out.props['half']

    def can_take(direction):
        other = blocks.get(add(pos, HORIZONTAL[direction]))
        return not (is_stairs(other) and other.props['facing'] == facing and other.props['half'] == half)

    behind = blocks.get(add(pos, HORIZONTAL[facing]))
    if is_stairs(behind) and behind.props['half'] == half:
        d = behind.props['facing']
        if d in (CCW[facing], CW[facing]) and can_take(OPPOSITE[d]):
            return 'outer_left' if d == CCW[facing] else 'outer_right'
    front = blocks.get(add(pos, HORIZONTAL[OPPOSITE[facing]]))
    if is_stairs(front) and front.props['half'] == half:
        d = front.props['facing']
        if d in (CCW[facing], CW[facing]) and can_take(d):
            return 'inner_left' if d == CCW[facing] else 'inner_right'
    return 'straight'


def connections(blocks):
    for pos, out in blocks.items():
        if out is None:
            continue
        if is_stairs(out):
            out.props['shape'] = stair_shape(blocks, pos, out)
        elif is_fence(out):
            for side, d in HORIZONTAL.items():
                other = blocks.get(add(pos, d))
                joined = is_fence(other) and (other.name.startswith('hbm:') == out.name.startswith('hbm:'))
                out.props[side] = 'true' if joined or sturdy(other, OPPOSITE[side]) else 'false'
        elif is_pane(out):
            for side, d in HORIZONTAL.items():
                other = blocks.get(add(pos, d))
                out.props[side] = 'true' if is_pane(other) or sturdy(other, OPPOSITE[side]) else 'false'
        elif out.name == 'hbm:sandbags':
            for side, d in HORIZONTAL.items():
                other = blocks.get(add(pos, d))
                joined = other is not None and (other.name == out.name or solid_render(other))
                out.props[side] = 'true' if joined else 'false'
        elif out.name == 'hbm:wood_barrier':
            facing = out.props['facing']
            for prop, side in (('front', facing), ('left', CCW[facing]), ('right', CW[facing])):
                out.props[prop] = 'true' if solid_render(blocks.get(add(pos, HORIZONTAL[side]))) else 'false'
        elif out.name == 'minecraft:grass_block':
            above = blocks.get(add(pos, (0, 1, 0)))
            out.props['snowy'] = 'true' if above is not None and above.name == 'minecraft:snow' else 'false'


def convert_one(name, used):
    src = nbtio.read(os.path.join(UPSTREAM, name + '.nbt'))
    palette = [(p['Name'].value, int(p['Properties']['meta'].value)) for p in src['palette']]
    size = [v.value for v in src['size']]
    if src.get('entities'):
        raise Unmapped('%s has entities' % name)
    if src.get('itemPalette'):
        raise Unmapped('%s has items in containers' % name)
    blocks = {}
    source_nbt = {}
    errors = []
    counts_in = collections.Counter()
    for b in src['blocks']:
        pos = tuple(v.value for v in b['pos'])
        block, meta = palette[b['state'].value]
        counts_in[block] += 1
        nbt = b.get('nbt', {})
        source_nbt[pos] = (block, meta, nbt)
        for key in ('items', 'Items'):
            if nbt.get(key):
                errors.append('%s %s %s has items' % (name, pos, block))
        try:
            if block.startswith('hbm:tile.'):
                out = hbm_block(block[len('hbm:tile.'):], meta, nbt, used)
            elif block.startswith('minecraft:'):
                out = vanilla_block(block[len('minecraft:'):], meta, nbt)
            else:
                raise Unmapped('unknown namespace')
        except (Unmapped, KeyError, IndexError) as ex:
            errors.append('%s %s meta %d: %s' % (block, pos, meta, ex))
            continue
        blocks[pos] = out
    if errors:
        return None, errors
    link_doors(blocks)
    connections(blocks)

    states = {}
    palette_out = TagList(nbtio.COMPOUND)
    blocks_out = TagList(nbtio.COMPOUND)
    counts_out = collections.Counter()
    state_list = []
    dropped = collections.Counter()
    expected = []
    for pos in sorted(blocks, key=lambda p: (p[1], p[0], p[2])):
        out = blocks[pos]
        if out is None:
            dropped[source_nbt[pos][0]] += 1
            continue
        key = (out.name, tuple(sorted(out.props.items())))
        if key not in states:
            states[key] = len(states)
            entry = {'Name': String(out.name)}
            if out.props:
                entry['Properties'] = {k: String(v) for k, v in sorted(out.props.items())}
            palette_out.append(entry)
            state_list.append({'name': out.name, 'props': out.props})
        counts_out[out.name] += 1
        block = {'pos': ints(*pos), 'state': Int(states[key])}
        if out.nbt:
            block['nbt'] = out.nbt
        blocks_out.append(block)
        expected.append({'pos': pos, 'state': states[key], 'be': nbtio.plain(out.nbt) if out.nbt else None})
    template = {'size': ints(*size), 'entities': TagList(nbtio.COMPOUND), 'blocks': blocks_out, 'palette': palette_out,
                'DataVersion': Int(DATA_VERSION)}
    nbtio.write(os.path.join(OUT_STRUCTURES, name + '.nbt'), template)
    return {'size': size, 'source_blocks': sum(counts_in.values()), 'template_blocks': len(blocks_out),
            'dropped_dummy_cells': dict(dropped), 'source_counts': dict(counts_in), 'template_counts': dict(counts_out),
            'states': state_list, 'expected': expected}, []


def write_table(name, base, low, top):
    table = {
        'type': 'minecraft:chest',
        'hbm:random_slots': True,
        'pools': [{
            'entries': [{'type': 'minecraft:loot_table', 'value': 'hbm:chests/' + base}],
            'rolls': {'type': 'minecraft:uniform', 'max': float(top), 'min': float(low)},
        }],
        'random_sequence': 'ntmextras:chests/' + name,
    }
    with open(os.path.join(OUT_TABLES, name + '.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump(table, f, indent=2)
        f.write('\n')


def main():
    os.makedirs(OUT_STRUCTURES, exist_ok=True)
    os.makedirs(OUT_TABLES, exist_ok=True)
    os.makedirs(os.path.dirname(REPORT), exist_ok=True)
    used = {}
    report = {}
    errors = []
    for name in STRUCTURES:
        result, err = convert_one(name, used)
        errors += err
        report[name] = result
    if errors:
        print('UNMAPPED:')
        for line in errors:
            print('  ' + line)
        raise SystemExit(1)
    for name, (base, low, top) in sorted(used.items()):
        write_table(name, base, low, top)
    report['_loot_tables_added'] = sorted(used)
    with open(REPORT, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(report, f, indent=1)
    for name in STRUCTURES:
        r = report[name]
        print('%-26s size %-14s source %6d template %6d dropped dummies %d' % (
            name, r['size'], r['source_blocks'], r['template_blocks'], sum(r['dropped_dummy_cells'].values())))
    print('loot tables added:', len(used))


if __name__ == '__main__':
    main()
