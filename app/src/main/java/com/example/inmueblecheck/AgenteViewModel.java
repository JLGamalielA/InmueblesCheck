package com.example.inmueblecheck;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AgenteViewModel extends AndroidViewModel {

    private final InmuebleRepository repository;
    private final LiveData<List<Inmueble>> catalogoCompleto;
    private final LiveData<List<Inmueble>> misPropiedades;

    // Filtros
    private final MutableLiveData<String> filtroTipo = new MutableLiveData<>(null); // "Venta", "Renta", null

    // Filtro de Rango de Precio (null = sin filtro)
    private final MutableLiveData<double[]> rangoPrecio = new MutableLiveData<>(null);

    public AgenteViewModel(@NonNull Application application) {
        super(application);
        repository = new InmuebleRepository(application);
        catalogoCompleto = repository.getCatalogoInmuebles();
        misPropiedades = repository.getMisInmuebles();
    }

    private LiveData<List<Inmueble>> aplicarFiltros(LiveData<List<Inmueble>> listaBase) {
        return Transformations.switchMap(listaBase, lista ->
                Transformations.switchMap(filtroTipo, tipo ->
                        Transformations.map(rangoPrecio, rango -> {
                            if (lista == null) return new ArrayList<>();

                            List<Inmueble> filtrada = new ArrayList<>();

                            for (Inmueble i : lista) {
                                boolean pasaTipo = (tipo == null) || (i.getTipoTransaccion() != null && i.getTipoTransaccion().equalsIgnoreCase(tipo));
                                boolean pasaPrecio = true;

                                if (rango != null) {
                                    double precio = i.getPrecio();
                                    double min = rango[0];
                                    double max = rango[1];

                                    if (max == -1) {
                                        // Rango: "X o más"
                                        if (precio < min) pasaPrecio = false;
                                    } else {
                                        // Rango: "Entre X y Y"
                                        if (precio < min || precio > max) pasaPrecio = false;
                                    }
                                }

                                if (pasaTipo && pasaPrecio) {
                                    filtrada.add(i);
                                }
                            }
                            return filtrada;
                        })
                )
        );
    }

    public LiveData<List<Inmueble>> getCatalogoFiltrado() { return aplicarFiltros(catalogoCompleto); }
    public LiveData<List<Inmueble>> getMisPropiedadesFiltradas() { return aplicarFiltros(misPropiedades); }

    // Setters
    public void setFiltroTipo(String tipo) { filtroTipo.setValue(tipo); }

    public void setRangoPrecio(double min, double max) {
        rangoPrecio.setValue(new double[]{min, max});
    }

    public void limpiarFiltroPrecio() {
        rangoPrecio.setValue(null);
    }

    public void recargarDatos() {
        String current = filtroTipo.getValue();
        filtroTipo.setValue(current);
    }

    // Compatibilidad
    public void aplicarFiltro(String tipo) { setFiltroTipo(tipo); }
    public LiveData<List<Inmueble>> getCatalogoInmuebles() { return getCatalogoFiltrado(); }
}