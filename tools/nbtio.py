# SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
# SPDX-License-Identifier: LGPL-3.0-only
"""Small reader and writer for gzipped NBT. Plain Python types with tag types kept next to the values:
compounds are dict, lists are TagList, numbers and strings are Tag unless the writer can tell the type itself."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTES, STRING, LIST, COMPOUND, INTS, LONGS = range(13)


class Tag:
    __slots__ = ('type', 'value')

    def __init__(self, type_, value):
        self.type = type_
        self.value = value

    def __repr__(self):
        return repr(self.value)

    def __eq__(self, other):
        return self.value == (other.value if isinstance(other, Tag) else other)

    def __hash__(self):
        return hash(self.value)


class TagList(list):
    __slots__ = ('elem',)

    def __init__(self, elem=END, items=()):
        super().__init__(items)
        self.elem = elem


def Byte(v):
    return Tag(BYTE, v)


def Short(v):
    return Tag(SHORT, v)


def Int(v):
    return Tag(INT, v)


def Float(v):
    return Tag(FLOAT, v)


def String(v):
    return Tag(STRING, v)


def ints(*values):
    return TagList(INT, [Int(v) for v in values])


class _Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def take(self, n):
        out = self.d[self.i:self.i + n]
        self.i += n
        return out

    def num(self, fmt):
        return struct.unpack('>' + fmt, self.take(struct.calcsize('>' + fmt)))[0]

    def string(self):
        return self.take(self.num('H')).decode('utf-8', 'surrogatepass')

    def payload(self, t):
        if t == BYTE:
            return Tag(BYTE, self.num('b'))
        if t == SHORT:
            return Tag(SHORT, self.num('h'))
        if t == INT:
            return Tag(INT, self.num('i'))
        if t == LONG:
            return Tag(LONG, self.num('q'))
        if t == FLOAT:
            return Tag(FLOAT, self.num('f'))
        if t == DOUBLE:
            return Tag(DOUBLE, self.num('d'))
        if t == BYTES:
            return Tag(BYTES, self.take(self.num('i')))
        if t == STRING:
            return Tag(STRING, self.string())
        if t == LIST:
            elem = self.num('b')
            n = self.num('i')
            return TagList(elem, [self.payload(elem) for _ in range(n)])
        if t == COMPOUND:
            out = {}
            while True:
                k = self.num('b')
                if k == END:
                    return out
                name = self.string()
                out[name] = self.payload(k)
        if t == INTS:
            n = self.num('i')
            return Tag(INTS, [self.num('i') for _ in range(n)])
        if t == LONGS:
            n = self.num('i')
            return Tag(LONGS, [self.num('q') for _ in range(n)])
        raise ValueError('bad tag type %d' % t)


def read(path):
    with gzip.open(path, 'rb') as f:
        r = _Reader(f.read())
    t = r.num('b')
    if t != COMPOUND:
        raise ValueError('root is not a compound')
    r.string()
    return r.payload(COMPOUND)


def _type_of(v):
    if isinstance(v, Tag):
        return v.type
    if isinstance(v, TagList):
        return LIST
    if isinstance(v, dict):
        return COMPOUND
    if isinstance(v, str):
        return STRING
    if isinstance(v, bool):
        return BYTE
    if isinstance(v, int):
        return INT
    raise TypeError(type(v))


def _write(out, t, v):
    if isinstance(v, Tag):
        v = v.value
    if t == BYTE:
        out.append(struct.pack('>b', int(v)))
    elif t == SHORT:
        out.append(struct.pack('>h', v))
    elif t == INT:
        out.append(struct.pack('>i', v))
    elif t == LONG:
        out.append(struct.pack('>q', v))
    elif t == FLOAT:
        out.append(struct.pack('>f', v))
    elif t == DOUBLE:
        out.append(struct.pack('>d', v))
    elif t == BYTES:
        out.append(struct.pack('>i', len(v)))
        out.append(bytes(v))
    elif t == STRING:
        raw = v.encode('utf-8', 'surrogatepass')
        out.append(struct.pack('>H', len(raw)))
        out.append(raw)
    elif t == LIST:
        elem = v.elem if isinstance(v, TagList) and v.elem != END else (_type_of(v[0]) if v else END)
        out.append(struct.pack('>bi', elem, len(v)))
        for x in v:
            _write(out, elem, x)
    elif t == COMPOUND:
        for k, x in v.items():
            xt = _type_of(x)
            out.append(struct.pack('>b', xt))
            _write(out, STRING, k)
            _write(out, xt, x)
        out.append(b'\x00')
    elif t == INTS:
        out.append(struct.pack('>i', len(v)))
        out.append(struct.pack('>%di' % len(v), *v))
    elif t == LONGS:
        out.append(struct.pack('>i', len(v)))
        out.append(struct.pack('>%dq' % len(v), *v))
    else:
        raise ValueError('bad tag type %d' % t)


def write(path, root):
    out = [struct.pack('>b', COMPOUND), struct.pack('>H', 0)]
    _write(out, COMPOUND, root)
    with open(path, 'wb') as f:
        with gzip.GzipFile(fileobj=f, mode='wb', mtime=0, filename='') as z:
            z.write(b''.join(out))


def plain(v):
    """Strips the tag types, for printing."""
    if isinstance(v, Tag):
        return v.value
    if isinstance(v, list):
        return [plain(x) for x in v]
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    return v
