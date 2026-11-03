# SPDX-FileCopyrightText: 2026 MC Warfare
# SPDX-License-Identifier: LGPL-3.0-only
"""Compiles a Wavefront OBJ into NTM NEXT's .hbm_mesh, the same data com.hbm.render.loader.ObjLoader would parse.

    py -3 -I obj2mesh.py in.obj out.hbm_mesh      compile
    py -3 -I obj2mesh.py --check a.obj b.hbm_mesh parse the OBJ, decode the mesh, compare group by group
"""
import math
import struct
import sys

MAGIC, VERSION = 0x48424D4F, 1


def f32(x):
    return struct.unpack('>f', struct.pack('>f', x))[0]


def parse(path):
    positions, uvs, normals, groups, current = [], [], [], [], None
    with open(path, encoding='utf-8') as fh:
        for raw in fh:
            line = raw.strip()
            if not line or line[0] == '#':
                continue
            head, _, rest = line.partition(' ')
            parts = rest.split()
            if head == 'v':
                positions.append(tuple(f32(float(p)) for p in parts[:3]))
            elif head == 'vn':
                normals.append(tuple(f32(float(p)) for p in parts[:3]))
            elif head == 'vt':
                uvs.append((f32(float(parts[0])), f32(1 - f32(float(parts[1])))))
            elif head in ('g', 'o'):
                if current is not None:
                    groups.append(current)
                current = {'name': parts[0], 'faces': []}
            elif head == 'f':
                if current is None:
                    current = {'name': 'Default', 'faces': []}
                corners = []
                for token in parts:
                    bits = token.split('/')
                    p = positions[int(bits[0]) - 1]
                    t = uvs[int(bits[1]) - 1] if len(bits) > 1 and bits[1] else (0.0, 0.0)
                    n = normals[int(bits[2]) - 1] if len(bits) > 2 and bits[2] else None
                    corners.append((p, t, n))
                assert 3 <= len(corners) <= 4, line
                fn = face_normal(corners[0][0], corners[1][0], corners[2][0])
                current['faces'].append((fn, [(p, t, n if n is not None else fn) for p, t, n in corners]))
    if current is not None:
        groups.append(current)
    return groups


def face_normal(a, b, c):
    ax, ay, az = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    bx, by, bz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
    nx, ny, nz = ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx
    length = math.sqrt(nx * nx + ny * ny + nz * nz)
    if length < 1e-6:
        length = 1.0
    return (f32(nx / length), f32(ny / length), f32(nz / length))


def bounds(group):
    pts = [c[0] for _, corners in group['faces'] for c in corners]
    return [min(p[i] for p in pts) for i in range(3)] + [max(p[i] for p in pts) for i in range(3)]


def varint(value):
    value &= 0xFFFFFFFF
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def delta(n):
    return varint((n << 1) ^ (n >> 31))


class Table:
    def __init__(self):
        self.index, self.rows = {}, []

    def at(self, row):
        if row not in self.index:
            self.index[row] = len(self.rows)
            self.rows.append(row)
        return self.index[row]

    def write(self, width):
        out = struct.pack('>i', len(self.rows))
        for row in self.rows:
            out += struct.pack('>%df' % width, *row)
        return out


def compile_mesh(groups):
    ps, ts, ns = Table(), Table(), Table()
    body = struct.pack('>i', len(groups))
    for g in groups:
        name = g['name'].encode('utf-8')
        body += struct.pack('>i', len(name)) + name + struct.pack('>6f', *bounds(g)) + struct.pack('>i', len(g['faces']))
        lp = lt = ln = lf = 0
        for fn, corners in g['faces']:
            body += bytes([len(corners)])
            i = ns.at(fn)
            body += delta(i - lf)
            lf = i
            for p, t, n in corners:
                ip, it, in_ = ps.at(p), ts.at(t), ns.at(n)
                body += delta(ip - lp) + delta(it - lt) + delta(in_ - ln)
                lp, lt, ln = ip, it, in_
    return struct.pack('>ii', MAGIC, VERSION) + ps.write(3) + ts.write(2) + ns.write(3) + body


def decode(data):
    at = [8]

    def i32():
        v = struct.unpack_from('>i', data, at[0])[0]
        at[0] += 4
        return v

    def floats(n):
        v = struct.unpack_from('>%df' % n, data, at[0])
        at[0] += 4 * n
        return v

    def rd():
        value, shift = 0, 0
        while True:
            b = data[at[0]]
            at[0] += 1
            value |= (b & 0x7F) << shift
            shift += 7
            if not b & 0x80:
                return (value >> 1) ^ -(value & 1)

    assert struct.unpack_from('>ii', data, 0) == (MAGIC, VERSION)
    p = floats(i32() * 3)
    t = floats(i32() * 2)
    n = floats(i32() * 3)
    groups = []
    for _ in range(i32()):
        ln = i32()
        name = data[at[0]:at[0] + ln].decode()
        at[0] += ln
        floats(6)
        faces, lp, lt, ln2, lf = [], 0, 0, 0, 0
        for _ in range(i32()):
            count = data[at[0]]
            at[0] += 1
            lf += rd()
            corners = []
            for _ in range(count):
                lp += rd()
                lt += rd()
                ln2 += rd()
                corners.append((p[lp * 3:lp * 3 + 3], t[lt * 2:lt * 2 + 2], n[ln2 * 3:ln2 * 3 + 3]))
            faces.append((n[lf * 3:lf * 3 + 3], corners))
        groups.append({'name': name, 'faces': faces})
    assert at[0] == len(data), 'trailing data'
    return groups


def same(a, b):
    return all(abs(x - y) < 1e-4 for x, y in zip(a, b))


def check(obj, mesh):
    left, right = parse(obj), decode(open(mesh, 'rb').read())
    assert [g['name'] for g in left] == [g['name'] for g in right], 'group names differ'
    total = 0
    for g, h in zip(left, right):
        assert len(g['faces']) == len(h['faces']), g['name']
        for (fa, ca), (fb, cb) in zip(g['faces'], h['faces']):
            assert same(fa, fb), ('face normal', g['name'], fa, fb)
            assert len(ca) == len(cb)
            for (p, t, n), (q, u, m) in zip(ca, cb):
                assert same(p, q) and same(t, u) and same(n, m), (g['name'], p, q, t, u, n, m)
            total += 1
    print('match: %d groups, %d faces' % (len(left), total))


if __name__ == '__main__':
    if sys.argv[1] == '--check':
        check(sys.argv[2], sys.argv[3])
    else:
        data = compile_mesh(parse(sys.argv[1]))
        decode(data)
        with open(sys.argv[2], 'wb') as out:
            out.write(data)
        print('%s: %d bytes' % (sys.argv[2], len(data)))
