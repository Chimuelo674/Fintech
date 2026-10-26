import calendar, sys
from datetime import date, timedelta

HOY = date(2026, 10, 6)
MESES = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto",
         "septiembre","octubre","noviembre","diciembre"]
RECARGO = {"EC-01":8500,"EC-02":5000,"EC-03":12000,"EC-04":3000,"EC-05":0,"EC-06":2000,"EC-07":6000,"EC-08":7500}
REGISTRA = {"EC-01":"EJ-01","EC-02":"EJ-02","EC-03":"EJ-03","EC-04":"PagoFlex","EC-05":"PagoFlex",
            "EC-06":"EJ-05","EC-07":"EJ-06","EC-08":"EJ-07"}

def ym(y, m, d):
    last = calendar.monthrange(y, m)[1]
    return date(y, m, min(d, last))

def meses(desde, hasta):
    y, m = desde
    while (y, m) <= hasta:
        yield y, m
        m += 1
        if m == 13: y, m = y + 1, 1

items = []
def add(user, emp, base, monto, venc, cuota=None, total=None, mes_en_concepto=True, late=False):
    concepto = f"{base} {MESES[venc.month-1]} {venc.year}" if mes_en_concepto else base
    items.append(dict(user=user, emp=emp, concepto=concepto, cuota=cuota, total=total,
                      monto=monto, venc=venc, late=late))

for i, (y, m) in enumerate([(2025,11),(2025,12)] + list(meses((2026,1),(2026,8)))):
    cuota = 11 + i if i < 2 else i - 1
    add("UF-01","EC-08","Arriendo departamento",520000, ym(y,m,10), cuota, 12)
    add("UF-01","EC-06","Cuota social",15000, ym(y,m,5), cuota, 12, late=(y,m)==(2026,3))
for y, m in meses((2026,4),(2026,8)):
    add("UF-01","EC-05","Aporte mensual",10000, ym(y,m,15))
for c, (y, m) in enumerate(meses((2026,3),(2026,8)), start=1):
    add("UF-01","EC-03","Crédito de consumo",86400, ym(y,m,25), c, 24, mes_en_concepto=False)

for c, (y, m) in enumerate(meses((2025,10),(2026,7)), start=3):
    add("UF-02","EC-01","Cuota pie departamento",310000, ym(y,m,31), c, 36,
        mes_en_concepto=False, late=c in (6, 10))

for i, (y, m) in enumerate([(2025,11),(2025,12)] + list(meses((2026,1),(2026,9)))):
    cuota = 11 + i if i < 2 else i - 1
    add("UF-03","EC-04","Seguro de hogar",18900, ym(y,m,1), cuota, 12)
for y, m in meses((2025,11),(2026,9)):
    add("UF-03","EC-04","Seguro de vida",9800, ym(y,m,1))
for y, m in meses((2026,5),(2026,9)):
    add("UF-03","EC-04","Seguro de salud complementario",12500, ym(y,m,1))

for c, (y, m) in enumerate(meses((2026,3),(2026,9)), start=1):
    add("UF-04","EC-07","Arancel",142000, ym(y,m,5), c, 10, late=c == 4)

for base, late_mes in (("Mensualidad", None), ("Mensualidad segundo hijo", (2025,11))):
    for c, (y, m) in zip((8,9,10), meses((2025,10),(2025,12))):
        add("UF-05","EC-02",base,185000, ym(y,m,10), c, 10, late=(y,m)==late_mes)
    for c, (y, m) in enumerate(meses((2026,3),(2026,8)), start=1):
        add("UF-05","EC-02",base,185000, ym(y,m,10), c, 10)
add("UF-05","EC-02","Matrícula 2026",210000, date(2026,2,27), mes_en_concepto=False)

for c, (y, m) in enumerate(meses((2025,10),(2026,8)), start=8):
    add("UF-06","EC-03","Crédito de consumo",54200, ym(y,m,20), c, 36,
        mes_en_concepto=False, late=c in (10, 15))
for y, m in meses((2026,6),(2026,8)):
    add("UF-06","EC-04","Seguro de hogar",16500, ym(y,m,1))

for c, (y, m) in enumerate(meses((2026,6),(2026,9)), start=6):
    add("UF-07","EC-06","Cuota social",15000, ym(y,m,5), c, 12)

items.sort(key=lambda x: (x["venc"], x["user"], x["emp"]))
for n, it in enumerate(items, start=1):
    it["folio"] = f"CP-H-{n:04d}"
    rec = RECARGO[it["emp"]] if it["late"] else 0
    it["recargo"] = rec
    if it["late"]:
        fh = f"{it['venc'] + timedelta(days=2)} 19:10"
    else:
        fh = f"{it['venc'] - timedelta(days=1)} {10 + n % 9:02d}:{(n*7) % 60:02d}"
    it["pago"] = dict(num=f"CPR-{n:06d}", fecha=fh, monto=it["monto"] + rec,
                      canal=["Portal de pagos", "Agente conversacional"][n % 2], atiempo=not it["late"])
    assert it["pago"]["num"] < "CPR-000377"

keys = [(i["user"], i["emp"], i["concepto"], i["cuota"]) for i in items]
assert len(keys) == len(set(keys)), "duplicado"

explicit = [
 ("UF-01","Pendiente","2026-10-10",None),("UF-01","Vencido","2026-10-05",None),
 ("UF-01","Pendiente","2026-10-15",None),("UF-01","Pagado","2026-09-25","2026-09-24"),
 ("UF-01","Pendiente","2026-10-25",None),("UF-02","Vencido","2026-09-30",None),
 ("UF-02","Pendiente","2026-10-30",None),("UF-03","Pagado","2026-10-01","2026-09-30"),
 ("UF-04","En revisión","2026-10-05",None),("UF-05","Pendiente","2026-10-10",None),
 ("UF-06","Anulado","2026-09-20",None),("UF-07","Pagado","2026-10-05","2026-10-04"),
 ("UF-02","Pagado","2026-08-31","2026-09-02"),("UF-01","Pagado","2026-09-10","2026-09-09"),
 ("UF-01","Pagado","2026-09-05","2026-09-07"),("UF-01","Pagado","2026-09-15","2026-09-15"),
 ("UF-05","Pagado","2026-09-10","2026-09-10"),
 ("UF-08","En revisión","2026-09-30",None),
]
rows = explicit + [(i["user"], "Pagado", str(i["venc"]), i["pago"]["fecha"][:10]) for i in items]

def add_months(d, n):
    m = d.month - 1 + n; y = d.year + m // 12; m = m % 12 + 1
    return date(y, m, min(d.day, calendar.monthrange(y, m)[1]))
desde = str(add_months(HOY, -12)); hoy = str(HOY)

esperado = {"UF-01":(36,33,91.7),"UF-02":(12,8,66.7),"UF-03":(28,28,100.0),"UF-04":(7,6,85.7),
            "UF-05":(20,19,95.0),"UF-06":(14,12,85.7),"UF-07":(5,5,100.0),"UF-08":(0,0,None)}
ok = True
for u, (n_e, a_e, p_e) in esperado.items():
    cons = [r for r in rows if r[0]==u and r[1] not in ("Anulado","En revisión") and desde < r[2] < hoy]
    at = sum(1 for r in cons if r[3] and r[3] <= r[2])
    p = None if len(cons) < 3 else round(at*1000/len(cons))/10
    flag = (len(cons), at, p) == (n_e, a_e, p_e)
    ok &= flag
    print(u, len(cons), at, p, "OK" if flag else f"ESPERADO {n_e} {a_e} {p_e}")
print("total sintéticos:", len(items))
assert ok

def kstr(s): return '"' + s.replace('"','\\"') + '"'
out = ["package cl.duocuc.pagoflex.data.seed", "",
 "import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity",
 "import cl.duocuc.pagoflex.data.local.entities.PagoEntity", "",
 "object HistorialSintetico {",
 "    val compromisos: List<CompromisoEntity> = listOf("]
cl, pl = [], []
for it in items:
    reg = REGISTRA[it["emp"]]
    fr = str(add_months(it["venc"], -1))
    cuota = str(it["cuota"]) if it["cuota"] else "null"
    total = str(it["total"]) if it["total"] else "null"
    cl.append(f'        CompromisoEntity({kstr(it["folio"])}, {kstr(it["folio"])}, {kstr(it["user"])}, {kstr(it["emp"])}, '
              f'{kstr(it["concepto"])}, {cuota}, {total}, {it["monto"]}.0, {kstr(str(it["venc"]))}, "Pagado", '
              f'{it["recargo"]}.0, {kstr(reg)}, {kstr(fr)})')
    p = it["pago"]
    pl.append(f'        PagoEntity({kstr(p["num"])}, {kstr(p["num"])}, {kstr(it["folio"])}, '
              f'{kstr(p["fecha"])}, {p["monto"]}.0, {kstr(p["canal"])}, "Pago simulado")')
out.append(",\n".join(cl)); out += ["    )", "", "    val pagos: List<PagoEntity> = listOf(", ",\n".join(pl), "    )", "}", ""]
open(sys.argv[1], "w", encoding="utf-8").write("\n".join(out))
print("escrito", sys.argv[1])
